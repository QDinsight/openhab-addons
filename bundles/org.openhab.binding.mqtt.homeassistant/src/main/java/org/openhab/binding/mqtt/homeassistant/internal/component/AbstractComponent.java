/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.mqtt.homeassistant.internal.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mqtt.generic.AvailabilityTracker;
import org.openhab.binding.mqtt.generic.ChannelStateUpdateListener;
import org.openhab.binding.mqtt.generic.MqttChannelStateDescriptionProvider;
import org.openhab.binding.mqtt.generic.TransformationServiceProvider;
import org.openhab.binding.mqtt.generic.values.Value;
import org.openhab.binding.mqtt.homeassistant.generic.internal.MqttBindingConstants;
import org.openhab.binding.mqtt.homeassistant.internal.ComponentChannel;
import org.openhab.binding.mqtt.homeassistant.internal.HaID;
import org.openhab.binding.mqtt.homeassistant.internal.component.ComponentFactory.ComponentConfiguration;
import org.openhab.binding.mqtt.homeassistant.internal.config.dto.AbstractChannelConfiguration;
import org.openhab.binding.mqtt.homeassistant.internal.config.dto.Availability;
import org.openhab.binding.mqtt.homeassistant.internal.config.dto.AvailabilityMode;
import org.openhab.binding.mqtt.homeassistant.internal.config.dto.Device;
import org.openhab.core.io.transport.mqtt.MqttBrokerConnection;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelGroupUID;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.type.ChannelDefinition;
import org.openhab.core.thing.type.ChannelGroupDefinition;
import org.openhab.core.thing.type.ChannelGroupType;
import org.openhab.core.thing.type.ChannelGroupTypeBuilder;
import org.openhab.core.thing.type.ChannelGroupTypeUID;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.CommandDescription;
import org.openhab.core.types.StateDescription;

import com.google.gson.Gson;

/**
 * A HomeAssistant component is comparable to a channel group.
 * It has a name and consists of multiple channels.
 *
 * @author David Graeff - Initial contribution
 * @param <C> Config class derived from {@link AbstractChannelConfiguration}
 */
@NonNullByDefault
public abstract class AbstractComponent<C extends AbstractChannelConfiguration> {
    private static final String JINJA_PREFIX = "JINJA:";

    // Component location fields
    protected final ComponentConfiguration componentConfiguration;
    protected final @Nullable ChannelGroupUID channelGroupUID;
    protected final HaID haID;

    // Channels and configuration
    protected final Map<String, ComponentChannel> channels = new TreeMap<>();
    protected final List<ComponentChannel> hiddenChannels = new ArrayList<>();

    // The hash code ({@link String#hashCode()}) of the configuration string
    // Used to determine if a component has changed.
    protected final int configHash;
    protected final String channelConfigurationJson;
    protected final C channelConfiguration;

    protected boolean configSeen;
    protected final boolean singleChannelComponent;
    protected final String groupId;
    protected final String uniqueId;

    public AbstractComponent(ComponentFactory.ComponentConfiguration componentConfiguration, Class<C> clazz,
            boolean newStyleChannels) {
        this(componentConfiguration, clazz, newStyleChannels, false);
    }

    /**
     * Creates component based on generic configuration and component configuration type.
     *
     * @param componentConfiguration generic componentConfiguration with not parsed JSON config
     * @param clazz target configuration type
     * @param newStyleChannels if new style channels should be used
     * @param singleChannelComponent if this component only ever has one channel, so should never be in a group
     *            (only if newStyleChannels is true)
     */
    public AbstractComponent(ComponentFactory.ComponentConfiguration componentConfiguration, Class<C> clazz,
            boolean newStyleChannels, boolean singleChannelComponent) {
        this.componentConfiguration = componentConfiguration;
        this.singleChannelComponent = newStyleChannels && singleChannelComponent;

        this.channelConfigurationJson = componentConfiguration.getConfigJSON();
        this.channelConfiguration = componentConfiguration.getConfig(clazz);
        this.configHash = channelConfigurationJson.hashCode();

        this.haID = componentConfiguration.getHaID();

        String name = channelConfiguration.getName();
        if (name != null && !name.isEmpty()) {
            groupId = this.haID.getGroupId(channelConfiguration.getUniqueId(), newStyleChannels);

            this.channelGroupUID = this.singleChannelComponent ? null
                    : new ChannelGroupUID(componentConfiguration.getThingUID(), groupId);
        } else {
            this.groupId = this.singleChannelComponent ? haID.component : "";
            this.channelGroupUID = null;
        }
        uniqueId = this.haID.getGroupId(channelConfiguration.getUniqueId(), false);

        this.configSeen = false;

        final List<Availability> availabilities = channelConfiguration.getAvailability();
        if (availabilities != null) {
            AvailabilityMode mode = channelConfiguration.getAvailabilityMode();
            AvailabilityTracker.AvailabilityMode availabilityTrackerMode = switch (mode) {
                case ALL -> AvailabilityTracker.AvailabilityMode.ALL;
                case ANY -> AvailabilityTracker.AvailabilityMode.ANY;
                case LATEST -> AvailabilityTracker.AvailabilityMode.LATEST;
            };
            componentConfiguration.getTracker().setAvailabilityMode(availabilityTrackerMode);
            for (Availability availability : availabilities) {
                String availabilityTemplate = availability.getValueTemplate();
                if (availabilityTemplate != null) {
                    availabilityTemplate = JINJA_PREFIX + availabilityTemplate;
                }
                componentConfiguration.getTracker().addAvailabilityTopic(availability.getTopic(),
                        availability.getPayloadAvailable(), availability.getPayloadNotAvailable(), availabilityTemplate,
                        componentConfiguration.getTransformationServiceProvider());
            }
        } else {
            String availabilityTopic = this.channelConfiguration.getAvailabilityTopic();
            if (availabilityTopic != null) {
                String availabilityTemplate = this.channelConfiguration.getAvailabilityTemplate();
                if (availabilityTemplate != null) {
                    availabilityTemplate = JINJA_PREFIX + availabilityTemplate;
                }
                componentConfiguration.getTracker().addAvailabilityTopic(availabilityTopic,
                        this.channelConfiguration.getPayloadAvailable(),
                        this.channelConfiguration.getPayloadNotAvailable(), availabilityTemplate,
                        componentConfiguration.getTransformationServiceProvider());
            }
        }
    }

    protected ComponentChannel.Builder buildChannel(String channelID, ChannelTypeUID channelTypeUID, Value valueState,
            String label, ChannelStateUpdateListener channelStateUpdateListener) {
        if (singleChannelComponent) {
            channelID = groupId;
        }
        return new ComponentChannel.Builder(this, channelID, channelTypeUID, valueState, label,
                channelStateUpdateListener);
    }

    public void setConfigSeen() {
        this.configSeen = true;
    }

    /**
     * Subscribes to all state channels of the component and adds all channels to the provided channel type provider.
     *
     * @param connection connection to the MQTT broker
     * @param scheduler thing scheduler
     * @param timeout channel subscription timeout
     * @return A future that completes as soon as all subscriptions have been performed. Completes exceptionally on
     *         errors.
     */
    public CompletableFuture<@Nullable Void> start(MqttBrokerConnection connection, ScheduledExecutorService scheduler,
            int timeout) {
        return Stream.concat(channels.values().stream(), hiddenChannels.stream())
                .map(v -> v.start(connection, scheduler, timeout)) //
                .reduce(CompletableFuture.completedFuture(null), (f, v) -> f.thenCompose(b -> v));
    }

    /**
     * Unsubscribes from all state channels of the component.
     *
     * @return A future that completes as soon as all subscriptions removals have been performed. Completes
     *         exceptionally on errors.
     */
    public CompletableFuture<@Nullable Void> stop() {
        return Stream.concat(channels.values().stream(), hiddenChannels.stream()) //
                .filter(Objects::nonNull) //
                .map(ComponentChannel::stop) //
                .reduce(CompletableFuture.completedFuture(null), (f, v) -> f.thenCompose(b -> v));
    }

    /**
     * Add all state and command descriptions to the state description provider.
     *
     * @param stateDescriptionProvider The state description provider
     */
    public void addStateDescriptions(MqttChannelStateDescriptionProvider stateDescriptionProvider) {
        channels.values().forEach(channel -> {
            StateDescription stateDescription = channel.getStateDescription();
            if (stateDescription != null) {
                stateDescriptionProvider.setDescription(channel.getChannel().getUID(), stateDescription);
            }
            CommandDescription commandDescription = channel.getCommandDescription();
            if (commandDescription != null) {
                stateDescriptionProvider.setDescription(channel.getChannel().getUID(), commandDescription);
            }
        });
    }

    public ChannelUID buildChannelUID(String channelID) {
        final ChannelGroupUID groupUID = channelGroupUID;
        if (groupUID != null) {
            return new ChannelUID(groupUID, channelID);
        }
        return new ChannelUID(componentConfiguration.getThingUID(), channelID);
    }

    public String getGroupId() {
        return groupId;
    }

    /**
     * Component (Channel Group) name.
     */
    public String getName() {
        String result = channelConfiguration.getName();

        Device device = channelConfiguration.getDevice();
        if (result == null && device != null) {
            result = device.getName();
        }
        if (result == null) {
            result = haID.objectID;
        }
        return result;
    }

    /**
     * Each component consists of multiple Channels.
     */
    public Map<String, ComponentChannel> getChannelMap() {
        return channels;
    }

    /**
     * Return a components channel. A HomeAssistant MQTT component consists of multiple functions
     * and those are mapped to one or more channels. The channel IDs are constants within the
     * derived Component, like the {@link Switch#SWITCH_CHANNEL_ID}.
     *
     * @param channelID The channel ID
     * @return A components channel
     */
    public @Nullable ComponentChannel getChannel(String channelID) {
        return channels.get(channelID);
    }

    /**
     * @return Returns the configuration hash value for easy comparison.
     */
    public int getConfigHash() {
        return configHash;
    }

    /**
     * Return the channel group type.
     */
    public @Nullable ChannelGroupType getChannelGroupType(String prefix) {
        if (channelGroupUID == null) {
            return null;
        }
        return ChannelGroupTypeBuilder.instance(getChannelGroupTypeUID(prefix), getName())
                .withChannelDefinitions(getAllChannelDefinitions()).build();
    }

    public List<ChannelDefinition> getChannelDefinitions() {
        if (channelGroupUID != null) {
            return List.of();
        }
        return getAllChannelDefinitions();
    }

    private List<ChannelDefinition> getAllChannelDefinitions() {
        return channels.values().stream().map(ComponentChannel::channelDefinition).toList();
    }

    public List<Channel> getChannels() {
        return channels.values().stream().map(ComponentChannel::getChannel).toList();
    }

    /**
     * Resets all channel states to state UNDEF. Call this method after the connection
     * to the MQTT broker got lost.
     */
    public void resetState() {
        channels.values().forEach(ComponentChannel::resetState);
    }

    /**
     * Return the channel group definition for this component.
     */
    public @Nullable ChannelGroupDefinition getGroupDefinition(String prefix) {
        if (channelGroupUID == null) {
            return null;
        }
        return new ChannelGroupDefinition(channelGroupUID.getId(), getChannelGroupTypeUID(prefix), getName(), null);
    }

    public boolean hasGroup() {
        return channelGroupUID != null;
    }

    public HaID getHaID() {
        return haID;
    }

    public String getChannelConfigurationJson() {
        return channelConfigurationJson;
    }

    @Nullable
    public TransformationServiceProvider getTransformationServiceProvider() {
        return componentConfiguration.getTransformationServiceProvider();
    }

    public boolean isEnabledByDefault() {
        return channelConfiguration.isEnabledByDefault();
    }

    public Gson getGson() {
        return componentConfiguration.getGson();
    }

    public C getChannelConfiguration() {
        return channelConfiguration;
    }

    private ChannelGroupTypeUID getChannelGroupTypeUID(String prefix) {
        return new ChannelGroupTypeUID(MqttBindingConstants.BINDING_ID, prefix + "_" + uniqueId);
    }
}
