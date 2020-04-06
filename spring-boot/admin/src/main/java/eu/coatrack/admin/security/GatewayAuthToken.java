package eu.coatrack.admin.security;

/*-
 * #%L
 * coatrack-admin
 * %%
 * Copyright (C) 2013 - 2020 Corizon | Institut für angewandte Systemtechnik Bremen GmbH (ATB)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * Represents an API key that was transmitted by an YGG gateway to access the
 * YGG admin api.
 *
 * @author gr-hovest
 */
public class GatewayAuthToken extends AbstractAuthenticationToken {

    private final String gatewayApiKey;
    private final String user;

    private final String password;

    public GatewayAuthToken(String gatewayApiKey, String user, String password, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.gatewayApiKey = gatewayApiKey;
        this.user = user;
        this.password = password;

    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }
    

    @Override
    public Object getCredentials() {
        return gatewayApiKey;
    }

    @Override
    public Object getPrincipal() {
        return gatewayApiKey;
    }

}
