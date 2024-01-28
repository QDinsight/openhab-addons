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
package org.openhab.persistence.mongodb.internal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mockito;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.items.*;
import org.openhab.core.library.types.*;
import org.openhab.core.persistence.FilterCriteria;
import org.openhab.core.types.State;
import org.osgi.framework.BundleContext;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.types.ObjectId;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;

/**
 * This class provides helper methods to create test items.
 * 
 * @author René Ulbricht - Initial contribution
 */
public class DataCreationHelper {

    /**
     * Creates a NumberItem with a given name and value.
     * 
     * @param name The name of the NumberItem.
     * @param value The value of the NumberItem.
     * @return The created NumberItem.
     */
    public static NumberItem createNumberItem(String name, Number value) {
        return createItem(NumberItem.class, name, new DecimalType(value));
    }

    /**
     * Creates a StringItem with a given name and value.
     * 
     * @param name The name of the StringItem.
     * @param value The value of the StringItem.
     * @return The created StringItem.
     */
    public static StringItem createStringItem(String name, String value) {
        return createItem(StringItem.class, name, new StringType(value));
    }

    /**
     * Creates an instance of a specific GenericItem subclass and sets its state.
     *
     * @param <T> The type of the item to create. This must be a subclass of GenericItem.
     * @param <S> The type of the state to set. This must be a subclass of State.
     * @param itemType The Class object representing the type of the item to create.
     * @param name The name to give to the new item.
     * @param state The state to set on the new item.
     * @return The newly created item.
     * @throws RuntimeException if an error occurs while creating the item or setting its state.
     */
    public static <T extends GenericItem, S extends State> T createItem(Class<T> itemType, String name, S state) {
        try {
            T item = itemType.getDeclaredConstructor(String.class).newInstance(name);
            item.setState(state);
            return item;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Provides a stream of arguments for parameterized tests. Each argument is an instance of a specific
     * GenericItem subclass with a set state.
     *
     * @return A stream of arguments for parameterized tests.
     */
    public static Stream<Arguments> provideOpenhabItemTypes() {
        return Stream.of(
                Arguments.of(
                        DataCreationHelper.createItem(StringItem.class, "StringItem", new StringType("StringValue"))),
                Arguments.of(DataCreationHelper.createItem(NumberItem.class, "NumberItem", new DecimalType(123.45))),
                Arguments.of(DataCreationHelper.createItem(DimmerItem.class, "DimmerItem", new PercentType(50))),
                Arguments.of(DataCreationHelper.createItem(SwitchItem.class, "SwitchItemON", OnOffType.ON)),
                Arguments.of(DataCreationHelper.createItem(SwitchItem.class, "SwitchItemOFF", OnOffType.OFF)),
                Arguments.of(DataCreationHelper.createItem(ContactItem.class, "ContactItemOPEN", OpenClosedType.OPEN)),
                Arguments.of(
                        DataCreationHelper.createItem(ContactItem.class, "ContactItemCLOSED", OpenClosedType.CLOSED)),
                Arguments.of(DataCreationHelper.createItem(RollershutterItem.class, "RollershutterItem",
                        new PercentType(30))),
                Arguments.of(DataCreationHelper.createItem(DateTimeItem.class, "DateTimeItem",
                        new DateTimeType(ZonedDateTime.now()))),
                Arguments.of(DataCreationHelper.createItem(ColorItem.class, "ColorItem", new HSBType("180,100,100"))),
                Arguments.of(
                        DataCreationHelper.createItem(LocationItem.class, "LocationItem", new PointType("51.0,0.0"))),
                Arguments.of(DataCreationHelper.createItem(PlayerItem.class, "PlayerItem", PlayPauseType.PLAY)),
                Arguments.of(DataCreationHelper.createItem(CallItem.class, "CallItem",
                        new StringListType("+49 123 456 789"))),
                Arguments.of(DataCreationHelper.createItem(ImageItem.class, "ImageItem",
                        new RawType(new byte[] { 0x00, 0x01, 0x02 }, "image/png"))),
                Arguments.of(DataCreationHelper.createItem(NumberItem.class, "NumberItemCelcius",
                        new QuantityType<>("25.00 °C"))));
    }

    /**
     * Provides a stream of arguments to be used for parameterized tests.
     *
     * Each argument is a DatabaseTestContainer instance. Some instances use a MemoryBackend,
     * while others use a MongoDBContainer with a specific MongoDB version.
     * In case there is no Docker available, only the MemoryBackend is used.
     *
     * @return A stream of Arguments, each containing a DatabaseTestContainer instance.
     */
    public static Stream<Arguments> provideDatabaseBackends() {
        if (DockerClientFactory.instance().isDockerAvailable()) {
            // If Docker is available, create a stream of Arguments with all backends
            return Stream.of(
                    // Create a DatabaseTestContainer with a MemoryBackend
                    Arguments.of(new DatabaseTestContainer(new MemoryBackend())),
                    // Create DatabaseTestContainers with MongoDBContainers of specific versions
                    Arguments.of(new DatabaseTestContainer("mongo:3.6")),
                    Arguments.of(new DatabaseTestContainer("mongo:4.4")),
                    Arguments.of(new DatabaseTestContainer("mongo:5.0")),
                    Arguments.of(new DatabaseTestContainer("mongo:6.0")));
        } else {
            // If Docker is not available, create a stream of Arguments with only the MemoryBackend
            return Stream.of(Arguments.of(new DatabaseTestContainer(new MemoryBackend())));
        }
    }

    /**
     * Creates a Document for a given item name, value, and timestamp.
     *
     * @param itemName The name of the item.
     * @param value The value of the item.
     * @param timestamp The timestamp of the item.
     * @return The created Document.
     */
    public static Document createDocument(String itemName, double value, LocalDate timestamp) {
        Document obj = new Document();
        obj.put(MongoDBFields.FIELD_ID, new ObjectId());
        obj.put(MongoDBFields.FIELD_ITEM, itemName);
        obj.put(MongoDBFields.FIELD_REALNAME, itemName);
        obj.put(MongoDBFields.FIELD_TIMESTAMP, timestamp);
        obj.put(MongoDBFields.FIELD_VALUE, value);
        return obj;
    }

    /**
     * Creates a FilterCriteria for a given item name.
     *
     * @param itemName The name of the item.
     * @return The created FilterCriteria.
     */
    public static FilterCriteria createFilterCriteria(String itemName) {
        return createFilterCriteria(itemName, null, null);
    }

    /**
     * Creates a FilterCriteria for a given item name, begin date, and end date.
     *
     * @param itemName The name of the item.
     * @param beginDate The begin date of the FilterCriteria.
     * @param endDate The end date of the FilterCriteria.
     * @return The created FilterCriteria.
     */
    public static FilterCriteria createFilterCriteria(String itemName, ZonedDateTime beginDate, ZonedDateTime endDate) {
        FilterCriteria filter = new FilterCriteria();
        filter.setItemName(itemName);
        filter.setPageSize(10);
        filter.setPageNumber(0);
        filter.setOrdering(FilterCriteria.Ordering.ASCENDING);
        filter.setBeginDate(beginDate);
        filter.setEndDate(endDate);
        return filter;
    }

    /**
     * Sets up a MongoDB instance for testing.
     *
     * @param collectionName The name of the MongoDB collection to be used for testing.
     * @param dbContainer The container running the MongoDB instance.
     * @return A SetupResult object containing the MongoDBPersistenceService, the database, the bundle context, the
     *         configuration map, the item registry, and the database name.
     */
    public static SetupResult setupMongoDB(String collectionName, DatabaseTestContainer dbContainer) {
        // Start the database container
        dbContainer.start();

        // Mock the ItemRegistry and BundleContext
        ItemRegistry itemRegistry = Mockito.mock(ItemRegistry.class);
        BundleContext bundleContext = Mockito.mock(BundleContext.class);

        // When getService is called on the bundleContext, return the mocked itemRegistry
        when(bundleContext.getService(any())).thenReturn(itemRegistry);

        // Create a new MongoDBPersistenceService instance
        MongoDBPersistenceService service = new MongoDBPersistenceService(itemRegistry);

        // Create a configuration map for the MongoDBPersistenceService
        Map<String, Object> config = new HashMap<>();
        config.put("url", dbContainer.getConnectionString());
        String dbname = UUID.randomUUID().toString();
        config.put("database", dbname);
        if (collectionName != null) {
            config.put("collection", collectionName);
        }

        // Create a MongoClient connected to the mock server
        MongoClient mongoClient = MongoClients.create(dbContainer.getConnectionString());

        // Create a database and collection
        MongoDatabase database = mongoClient.getDatabase(dbname);

        // Setup logger to capture log events
        Logger logger = (Logger) LoggerFactory.getLogger(MongoDBPersistenceService.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        logger.setLevel(Level.WARN);

        // Return a SetupResult object containing the service, database, bundle context, config, item registry, and
        // database name
        return new SetupResult(service, database, bundleContext, config, itemRegistry, dbname);
    }

    /**
     * Sets up a logger to capture log events.
     *
     * @param loggerClass The class that the logger is for.
     * @param level The level of the logger.
     * @return The list appender attached to the logger.
     */
    public static ListAppender<ILoggingEvent> setupLogger(Class<?> loggerClass, Level level) {
        Logger logger = (Logger) LoggerFactory.getLogger(loggerClass);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        logger.setLevel(level); // Set log level
        return listAppender;
    }

        private static Object convertValue(State state) {
            Object value;
            if (state instanceof PercentType) {
                PercentType type = (PercentType) state;
                value = type.toBigDecimal().doubleValue();
            } else if (state instanceof DateTimeType) {
                DateTimeType type = (DateTimeType) state;
                value = Date.from(type.getZonedDateTime().toInstant());
            } else if (state instanceof DecimalType) {
                DecimalType type = (DecimalType) state;
                value = type.toBigDecimal().doubleValue();
            } else {
                value = state.toString();
            }
            return value;
        }

        public static void storeOldData(MongoCollection<Document> collection, String realItemName, State state) {
            // use the old way to store data
            Object value = convertValue(state);

            Document obj = new Document();
            obj.put(MongoDBFields.FIELD_ID, new ObjectId());
            obj.put(MongoDBFields.FIELD_ITEM, realItemName);
            obj.put(MongoDBFields.FIELD_REALNAME, realItemName);
            obj.put(MongoDBFields.FIELD_TIMESTAMP, new Date());
            obj.put(MongoDBFields.FIELD_VALUE, value);
            collection.insertOne(obj);
        }
}
