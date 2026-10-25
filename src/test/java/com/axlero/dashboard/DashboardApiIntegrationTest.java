package com.axlero.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:dashboard-api-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class DashboardApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void persistsDashboardWidgetsAndRestrictsAccessToTheirOwner() throws Exception {
        MockHttpSession ownerSession = registerAndLogin(uniqueEmail());
        MvcResult created = mockMvc.perform(post("/api/dashboards")
                        .with(csrf())
                        .session(ownerSession)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Revenue overview",
                                  "widgets": [{
                                    "title": "Monthly revenue",
                                    "chartType": "area",
                                    "metricType": "sum",
                                    "groupByField": "month",
                                    "valueField": "revenue",
                                    "position": 0
                                  }]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Revenue overview"))
                .andExpect(jsonPath("$.widgets[0].title").value("Monthly revenue"))
                .andReturn();
        long dashboardId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(get("/api/dashboards").session(ownerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(dashboardId))
                .andExpect(jsonPath("$[0].widgets[0].position").value(0));

        mockMvc.perform(put("/api/dashboards/{id}", dashboardId)
                        .with(csrf())
                        .session(ownerSession)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Updated overview",
                                  "widgets":[{
                                    "title":"Orders by week",
                                    "chartType":"bar",
                                    "metricType":"count",
                                    "groupByField":"week",
                                    "valueField":"orders",
                                    "position":0
                                  }]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated overview"))
                .andExpect(jsonPath("$.widgets").isArray())
                .andExpect(jsonPath("$.widgets.length()").value(1))
                .andExpect(jsonPath("$.widgets[0].title").value("Orders by week"));

        MockHttpSession otherSession = registerAndLogin(uniqueEmail());
        mockMvc.perform(get("/api/dashboards/{id}", dashboardId).session(otherSession))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/dashboards/{id}", dashboardId)
                        .with(csrf())
                        .session(ownerSession))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/dashboards/{id}", dashboardId).session(ownerSession))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsUnsupportedChartTypes() throws Exception {
        MockHttpSession session = registerAndLogin(uniqueEmail());

        mockMvc.perform(post("/api/dashboards")
                        .with(csrf())
                        .session(session)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Invalid dashboard",
                                  "widgets": [{
                                    "title": "Unsupported",
                                    "chartType": "unsupported",
                                    "metricType": "sum",
                                    "groupByField": "month",
                                    "valueField": "revenue",
                                    "position": 0
                                  }]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    private MockHttpSession registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name":"Dashboard Tester","email":"%s","password":"strong-password"}
                                """.formatted(email)))
                .andExpect(status().isCreated());

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"strong-password"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) login.getRequest().getSession(false);
    }

    private String uniqueEmail() {
        return "dashboard-" + UUID.randomUUID() + "@example.com";
    }
}
