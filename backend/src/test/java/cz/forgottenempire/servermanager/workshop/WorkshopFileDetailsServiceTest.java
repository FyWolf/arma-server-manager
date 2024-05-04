package cz.forgottenempire.servermanager.workshop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkshopFileDetailsServiceTest {

    private static final String STEAM_API_KEY = "ABCD1234";
    private static final long NON_EXISTING_MOD_ID = 1L;
    private static final long MOD_ID = 2L;

    @Mock(stubOnly = true)
    private RestTemplate restTemplate;
    @Mock(stubOnly = true)
    private ResponseEntity<String> response;

    private WorkshopFileDetailsService fileDetailsService;

    @BeforeEach
    void setUp() {
        fileDetailsService = new WorkshopFileDetailsService(STEAM_API_KEY, restTemplate);
    }

    @Test
    void whenGettingModNameOfExistingPublicMod_thenModNameReturned() {
        when(response.getBody()).thenReturn(
                """
                        {
                          "response": {
                            "publishedfiledetails": [
                              {
                                "title": "Mod Name"
                              }
                            ]
                          }
                        }
                        """);
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class))).thenReturn(response);

        String modName = fileDetailsService.getModName(MOD_ID);

        assertThat(modName).isEqualTo("Mod Name");
    }

    @Test
    void whenGettingModNameOfNonExistingMod_thenNullReturned() {
        when(response.getBody()).thenReturn(
                """
                        {
                          "response": {
                              "publishedfiledetails": []
                          }
                        }
                        """);
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class))).thenReturn(response);

        String modName = fileDetailsService.getModName(NON_EXISTING_MOD_ID);

        assertThat(modName).isNull();
    }

    @Test
    void whenGettingAppIdOfExistingPublicMod_thenAppIdReturned() {
        when(response.getBody()).thenReturn(
                """
                        {
                          "response": {
                            "publishedfiledetails": [
                              {
                                "consumer_app_id": "107410"
                              }
                            ]
                          }
                        }
                        """);
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class))).thenReturn(response);

        Long appId = fileDetailsService.getModAppId(MOD_ID);

        assertThat(appId).isEqualTo(107410L);
    }

    @Test
    void whenGettingAppIdOfNonExistingMod_thenNullReturned() {
        when(response.getBody()).thenReturn(
                """
                        {
                          "response": {
                              "publishedfiledetails": []
                          }
                        }
                        """);
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class))).thenReturn(response);

        Long appId = fileDetailsService.getModAppId(NON_EXISTING_MOD_ID);

        assertThat(appId).isNull();
    }
}
