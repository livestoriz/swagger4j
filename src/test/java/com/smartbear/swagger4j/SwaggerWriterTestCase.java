/**
 *  Copyright 2013 SmartBear Software, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.smartbear.swagger4j;

import com.smartbear.swagger4j.impl.Constants;
import junit.framework.TestCase;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;

import static com.smartbear.swagger4j.impl.Utils.MapSwaggerStore;

public class SwaggerWriterTestCase extends TestCase {

    public void testCreationFromJson() throws Exception {
        SwaggerFactory factory = Swagger.createSwaggerFactory();
        SwaggerReader reader = Swagger.createReader();
        ResourceListing resourceListing = reader.readResourceListing(URI.create(
                TestUtils.getTestJsonResourceListingUrl( SwaggerVersion.V1_1)));

        testCreation(factory, resourceListing);
    }

    public void testCreationFromXml() throws Exception {
        SwaggerFactory factory = Swagger.createSwaggerFactory();
        ResourceListing resourceListing = Swagger.createReader().readResourceListing(URI.create(
                TestUtils.getTestXmlResourceListingUrl( SwaggerVersion.V1_1)));

        testCreation(factory, resourceListing);
    }

    public void testJsonCreation() throws Exception {
        MapSwaggerStore store = new MapSwaggerStore();
        ResourceListing resourceListing = createResourceListingForCreationTesting(store, SwaggerFormat.json);

        String json = store.getFileMap().get("api-docs.json").toString();
        assertTrue(json.length() > 0);

        JsonReader reader = Json.createReader(new StringReader(json));
        SwaggerVersion swaggerVersion = resourceListing.getSwaggerVersion();
        assertEquals(swaggerVersion.getIdentifier(), reader.readObject().getString(Constants.SWAGGER_VERSION));

        System.out.println( json );

        json = store.getFileMap().get("api-docs/user.json").toString();
        System.out.println("Created: " + json);
        reader = Json.createReader(new StringReader(json));
        JsonObject object = reader.readObject();
        assertEquals(swaggerVersion.getIdentifier(), object.getString(Constants.SWAGGER_VERSION));

        Constants constants = Constants.get(swaggerVersion);

        JsonArray apis = object.getJsonArray(constants.APIS);
        assertEquals(1, apis.size());

        object = apis.getJsonObject(0).getJsonArray(constants.OPERATIONS).getJsonObject(0);
        JsonArray produces = object.getJsonArray(constants.PRODUCES);
        assertEquals(1, produces.size());
        assertEquals("text/xml", produces.getString(0));
        JsonArray consumes = object.getJsonArray(constants.CONSUMES);
        assertEquals(1, consumes.size());
        assertEquals("application/json", consumes.getString(0));
        assertEquals("Testing summary", object.getString(constants.SUMMARY));

        System.out.println(json);
    }

    private ResourceListing createResourceListingForCreationTesting(MapSwaggerStore store, SwaggerFormat format) throws IOException {
        ResourceListing resourceListing = Swagger.createResourceListing(SwaggerVersion.DEFAULT_VERSION);
        ApiDeclaration apiDeclaration = Swagger.createApiDeclaration("http://petstore.swagger.wordnik.com/api", "/user");

        Api api = apiDeclaration.addApi("/user.{format}/createWithArray");
        Operation operation = api.addOperation("createUsersWithArrayInput", Operation.Method.POST);

        operation.setSummary("Testing summary");
        operation.addProduces("text/xml");
        operation.addConsumes("application/json");
        operation.addResponseMessage(400, "not found");
        operation.addParameter("test", Parameter.ParamType.query).setDescription("Test parameter");

        resourceListing.addApi(apiDeclaration, "/user.{format}");

        resourceListing.getInfo().setTitle("Testing Resource Listing");

        SwaggerWriter swaggerWriter = Swagger.createWriter(format);
        swaggerWriter.writeSwagger(store, resourceListing);

        assertEquals(2, store.getFileMap().size());

        return resourceListing;
    }

    public void testXmlCreation() throws Exception {
        MapSwaggerStore store = new MapSwaggerStore();
        ResourceListing resourceListing = createResourceListingForCreationTesting(store, SwaggerFormat.xml);

        String xml = store.getFileMap().get("api-docs.xml").toString();
        assertTrue(xml.length() > 0);

        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource( new StringReader(xml)));
        Element documentElement = document.getDocumentElement();
        assertEquals(Constants.API_DOCUMENTATION, documentElement.getNodeName());
        Element elm = (Element) documentElement.getElementsByTagName(Constants.SWAGGER_VERSION).item(0);
        SwaggerVersion swaggerVersion = resourceListing.getSwaggerVersion();
        assertEquals(swaggerVersion.getIdentifier(), elm.getTextContent());

        xml = store.getFileMap().get("api-docs/user.xml").toString();
        document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        documentElement = document.getDocumentElement();
        assertEquals(Constants.API_DOCUMENTATION, documentElement.getNodeName());

        elm = (Element) documentElement.getElementsByTagName(Constants.SWAGGER_VERSION).item(0);
        assertEquals(swaggerVersion.getIdentifier(), elm.getTextContent());

        Constants constants = Constants.get(swaggerVersion);

        NodeList nl = documentElement.getElementsByTagName(constants.APIS);
        assertEquals(1, nl.getLength());

        elm = (Element) ((Element) nl.item(0)).getElementsByTagName(constants.OPERATIONS).item(0);
        assertEquals(1, elm.getElementsByTagName(constants.PRODUCES).getLength());

        assertEquals("Testing summary", elm.getElementsByTagName(constants.SUMMARY).item(0).getTextContent());
        System.out.println(xml);
    }

    private MapSwaggerStore testCreation(SwaggerFactory factory, ResourceListing resourceListing) throws IOException {
        SwaggerWriter swaggerWriter = factory.createSwaggerWriter(SwaggerFormat.xml);
        MapSwaggerStore store = new MapSwaggerStore();
        swaggerWriter.writeSwagger(store, resourceListing);

        assertEquals(3, store.getFileMap().size());
        assertTrue(store.getFileMap().containsKey("api-docs.xml"));

        swaggerWriter = factory.createSwaggerWriter(SwaggerFormat.json);
        swaggerWriter.writeSwagger(store, resourceListing);

        assertEquals(6, store.getFileMap().size());
        assertTrue(store.getFileMap().containsKey("api-docs.json"));

        return store;
    }

    public void testSimpleCreation() throws Exception
    {
        SwaggerFactory factory = Swagger.createSwaggerFactory();

        ApiDeclaration apiDeclaration = factory.createApiDeclaration( "http://api.mycompany.com/apis", "/user" );
        Api api = apiDeclaration.addApi( "{id}" );
        Operation op = api.addOperation( "getuserbyid", Operation.Method.GET );
        op.addParameter( "id", Parameter.ParamType.path );

        ResourceListing rl = factory.createResourceListing(SwaggerVersion.DEFAULT_VERSION );
        rl.setApiVersion( "1.0" );
        rl.addApi( apiDeclaration, "user-doc.{format}" );

        Swagger.writeSwagger( rl, "target/test-export" );
    }
}
