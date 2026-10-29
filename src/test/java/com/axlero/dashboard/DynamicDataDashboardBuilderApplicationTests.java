package com.axlero.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.h2.console.enabled=true")
@AutoConfigureMockMvc
class DynamicDataDashboardBuilderApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void dashboardApiRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/dashboards"))
				.andExpect(status().isForbidden());
	}

}
