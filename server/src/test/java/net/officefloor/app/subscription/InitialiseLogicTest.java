/*
 * OfficeFloor - http://www.officefloor.net
 * Copyright (C) 2005-2019 Daniel Sagenschneider
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.officefloor.app.subscription;

import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.officefloor.app.subscription.InitialiseLogic.Initialisation;
import net.officefloor.app.subscription.store.Administration;
import net.officefloor.nosql.objectify.mock.ObjectifyRule;
import net.officefloor.server.http.HttpException;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.server.http.mock.MockHttpResponse;
import net.officefloor.web.json.JacksonHttpObjectResponderFactory;
import net.officefloor.woof.mock.MockWoofServer;
import net.officefloor.woof.mock.MockWoofServerRule;

/**
 * Tests the {@link InitialiseLogic}.
 * 
 * @author Daniel Sagenschneider
 */
public class InitialiseLogicTest {

	private ObjectifyRule obectify = new ObjectifyRule();

	private MockWoofServerRule server = new MockWoofServerRule();

	@Rule
	public RuleChain chain = RuleChain.outerRule(this.obectify).around(this.server);

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	public void retrieveInitialisation() throws Exception {

		// Store configuration
		Administration administration = new Administration("MOCK_GOOGLE_CLIENT_ID", "MOCK_PAYPAL_ENVIRONMENT",
				"MOCK_PAYPAL_CLIENT_ID", "MOCK_PAYPAL_CLIENT_SECRET", "MOCK_PAYPAL_CURRENCY");
		this.obectify.store(administration);

		// Ensure able to obtain initialisation from configuration
		MockHttpResponse response = this.server
				.send(MockWoofServer.mockRequest("/initialise").header("Accept", "application/json"));
		response.assertResponse(200, mapper.writeValueAsString(
				new Initialisation(true, "MOCK_GOOGLE_CLIENT_ID", "MOCK_PAYPAL_CLIENT_ID", "MOCK_PAYPAL_CURRENCY")));
	}

	@Test
	public void notYetInitialised() throws Exception {
		MockHttpResponse response = this.server
				.send(MockWoofServer.mockRequest("/initialise").header("Accept", "application/json"));
		response.assertResponse(404, JacksonHttpObjectResponderFactory
				.getEntity(new HttpException(HttpStatus.NOT_FOUND, "Application not configured"), mapper));
	}

	@Test
	public void startupInitialisation() throws Exception {

		// Specify location of configuration file
		Path path = Paths.get(".", "src/test/resources", this.getClass().getPackage().getName().replace('.', '/'),
				"administration.json");
		assertTrue("INVALID TEST: can not find intialisation file", Files.exists(path));

		// Ensure reset property
		String initialiseFilePath = System.getProperty(InitialiseLogic.PROPERTY_INITIALISE_FILE_PATH);
		System.setProperty(InitialiseLogic.PROPERTY_INITIALISE_FILE_PATH, path.toAbsolutePath().toString());
		try {

			// Ensure initialise from file
			MockHttpResponse response = this.server
					.send(MockWoofServer.mockRequest("/initialise").header("Accept", "application/json"));
			response.assertResponse(200, mapper.writeValueAsString(new Initialisation(true, "MOCK_GOOGLE_CLIENT_ID",
					"MOCK_PAYPAL_CLIENT_ID", "MOCK_PAYPAL_CURRENCY")));

		} finally {
			if (initialiseFilePath == null) {
				System.clearProperty(InitialiseLogic.PROPERTY_INITIALISE_FILE_PATH);
			} else {
				System.setProperty(InitialiseLogic.PROPERTY_INITIALISE_FILE_PATH, initialiseFilePath);
			}
		}
	}

}