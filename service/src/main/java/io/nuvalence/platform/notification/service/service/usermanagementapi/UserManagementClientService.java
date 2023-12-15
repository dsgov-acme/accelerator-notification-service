package io.nuvalence.platform.notification.service.service.usermanagementapi;

import io.nuvalence.platform.notification.usermanagent.client.ApiClient;
import io.nuvalence.platform.notification.usermanagent.client.ApiException;
import io.nuvalence.platform.notification.usermanagent.client.generated.api.UsersApi;
import io.nuvalence.platform.notification.usermanagent.client.generated.models.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing user management client.
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class UserManagementClientService {

    @Value("${userManagement.baseUrl}")
    private String baseUrl;

    private final AuthTokenProvider authTokenProvider;

    private UsersApi createUsersApi() {
        final ApiClient apiClient = new ApiClient();
        apiClient.updateBaseUri(baseUrl);
        apiClient.setRequestInterceptor(
                request ->
                        request.header("authorization", "Bearer " + authTokenProvider.getToken()));

        return new UsersApi(apiClient);
    }

    /**
     * Get user.
     *
     * @param userId id of the user.
     * @return User.
     * @throws ApiException for possible errors reaching user management service.
     */
    public Optional<UserDTO> getUser(UUID userId) throws ApiException {
        UsersApi api = createUsersApi();
        try {
            return Optional.of(api.getUserById(userId));
        } catch (ApiException e) {
            log.error("Error getting user from user management service", e);
            return Optional.empty();
        }
    }
}
