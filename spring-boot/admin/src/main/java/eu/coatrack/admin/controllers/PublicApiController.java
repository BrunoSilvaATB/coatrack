package eu.coatrack.admin.controllers;

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

import eu.coatrack.admin.logic.CreateApiKeyAction;
import eu.coatrack.admin.model.repository.ServiceApiRepository;
import eu.coatrack.admin.model.repository.UserRepository;
import eu.coatrack.api.*;

import org.modelmapper.Conditions;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.modelmapper.convention.MatchingStrategies.STRICT;

@RestController
@RequestMapping(value = "/public-api")
@Component
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST})
public class PublicApiController implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(PublicApiController.class);

    @Value("${ygg.admin.server.url}")
    private String coatrackAdminPublicServerURL;

    @Autowired
    private ServiceApiRepository serviceApiRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CreateApiKeyAction createApiKeyAction;

    @Autowired
    private ReportController reportController;

    private ModelMapper modelMapper;

    public void afterPropertiesSet() throws Exception {
        modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(STRICT);
        modelMapper.getConfiguration().setPropertyCondition(Conditions.isNotNull());
    }

    @GetMapping(value = "/services/{serviceOwnerUsername}/{uriIdentifier}", produces = "application/json")
    public ServiceApiDTO findByServiceUriIdentifier(@PathVariable("uriIdentifier") String uriIdentifier, @PathVariable("serviceOwnerUsername") String serviceOwnerUsername) {
        return toDTO(serviceApiRepository.findServiceApiByServiceOwnerAndUriIdentifier(serviceOwnerUsername, uriIdentifier));
    }

    @GetMapping(value = "/services", produces = "application/json")
    public List<ServiceApiDTO> findByServiceOwner() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return toListOfDTOs(serviceApiRepository.findByOwnerUsername(auth.getName()));
    }

    @PostMapping(value = "services/{serviceOwnerUsername}/{uriIdentifier}/subscriptions", produces = "application/json")
    public String subscribeService(@PathVariable("uriIdentifier") String uriIdentifier, @PathVariable("serviceOwnerUsername") String serviceOwnerUsername) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User userWhoSubscribes = userRepository.findByUsername(auth.getName());
        ServiceApi serviceToSubscribeTo = serviceApiRepository.findServiceApiByServiceOwnerAndUriIdentifier(serviceOwnerUsername, uriIdentifier);
        createApiKeyAction.setServiceApi(serviceToSubscribeTo);
        createApiKeyAction.setUser(userWhoSubscribes);
        createApiKeyAction.execute();
        String baseURL = coatrackAdminPublicServerURL + "/admin/api-keys";
        return baseURL;
    }

    @GetMapping(value = "services/{serviceOwnerUsername}/{uriIdentifier}/usageStatistics")
    public ServiceUsageStatisticsDTO getServiceUsageStatistics(@PathVariable("uriIdentifier") String uriIdentifier, @PathVariable("serviceOwnerUsername") String serviceOwnerUsername, @RequestParam String dateFrom, @RequestParam String dateUntil) throws IOException, ParseException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        ServiceApi service = serviceApiRepository.findServiceApiByServiceOwnerAndUriIdentifier(serviceOwnerUsername, uriIdentifier);
        Long userId = userRepository.findByUsername(auth.getName()).getId();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Date dateFromParsedToDate = formatter.parse(dateFrom);
        Date dateUntilParsedToDate = formatter.parse(dateUntil);
        List<ApiUsageReport> apiUsageReports = new ArrayList<>();
        // if user is the owner of the service
        if (serviceOwnerUsername.equals(auth.getName())) {

            apiUsageReports.addAll(reportController.calculateApiUsageReportForSpecificService(service, -1L, // for all consumers
                    java.sql.Date.valueOf(dateFromParsedToDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()),
                    java.sql.Date.valueOf(dateUntilParsedToDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()),
                    true));
        } else {
            apiUsageReports = reportController.calculateApiUsageReportForSpecificService(service, userId, dateFromParsedToDate, dateUntilParsedToDate, false);
        }

        ServiceUsageStatisticsDTO serviceUsageStatisticsDTO = new ServiceUsageStatisticsDTO();
        serviceUsageStatisticsDTO.setNumberOfCalls(apiUsageReports.stream().mapToLong(ApiUsageReport::getCalls).sum());
        serviceUsageStatisticsDTO.setDateFrom(dateFrom);
        serviceUsageStatisticsDTO.setDateUntil(dateUntil);
        serviceUsageStatisticsDTO.setUriIdentifier(uriIdentifier);

        return serviceUsageStatisticsDTO;
    }

    List<ServiceApiDTO> toListOfDTOs(List<ServiceApi> entity) {

        List<ServiceApiDTO> serviceApiDTOList = new ArrayList<>();
        int counter = 0;
        for (ServiceApi singleEntity : entity) {
            serviceApiDTOList.add(modelMapper.map(singleEntity, ServiceApiDTO.class));
            serviceApiDTOList.get(counter).setServiceOwnerUsername(singleEntity.getOwner().getUsername());
            counter++;
        }
        return serviceApiDTOList;
    }

    ServiceApiDTO toDTO(ServiceApi entity) {
        ServiceApiDTO serviceDTO = modelMapper.map(entity, ServiceApiDTO.class);
        serviceDTO.setServiceOwnerUsername(entity.getOwner().getUsername());
        return serviceDTO;
    }
}