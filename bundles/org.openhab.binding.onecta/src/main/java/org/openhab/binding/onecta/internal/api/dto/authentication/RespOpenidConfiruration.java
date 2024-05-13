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
package org.openhab.binding.onecta.internal.api.dto.authentication;

import java.util.ArrayList;

import com.google.gson.annotations.SerializedName;

/**
 * @author Alexander Drent - Initial contribution
 */
public class RespOpenidConfiruration {
    @SerializedName("authorization_endpoint")
    public String authorizationEndpoint;
    @SerializedName("id_token_signing_alg_values_supported")
    public ArrayList<String> idTokenSigningAlgValuesSupported;
    @SerializedName("issuer")
    public String issuer;
    @SerializedName("jwks_uri")
    public String jwksUri;
    @SerializedName("response_types_supported")
    public ArrayList<String> responseTypesSupported;
    @SerializedName("scopes_supported")
    public ArrayList<String> scopesSupported;
    @SerializedName("subject_types_supported")
    public ArrayList<String> subjectTypesSupported;
    @SerializedName("token_endpoint")
    public String tokenEndpoint;
    @SerializedName("token_endpoint_auth_methods_supported")
    public ArrayList<String> tokenEndpointAuthMethodsSupported;
    @SerializedName("userinfo_endpoint")
    public String userinfoEndpoint;

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public ArrayList<String> getIdTokenSigningAlgValuesSupported() {
        return idTokenSigningAlgValuesSupported;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public ArrayList<String> getResponseTypesSupported() {
        return responseTypesSupported;
    }

    public ArrayList<String> getScopesSupported() {
        return scopesSupported;
    }

    public ArrayList<String> getSubjectTypesSupported() {
        return subjectTypesSupported;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public ArrayList<String> getTokenEndpointAuthMethodsSupported() {
        return tokenEndpointAuthMethodsSupported;
    }

    public String getUserinfoEndpoint() {
        return userinfoEndpoint;
    }
}
