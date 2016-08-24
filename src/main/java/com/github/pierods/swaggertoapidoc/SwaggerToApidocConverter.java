/**
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

 */

package com.github.pierods.swaggertoapidoc;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.swagger.models.HttpMethod;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.RefModel;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.FormParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.parser.SwaggerParser;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 * Created by piero on 4/26/16.
 */
public class SwaggerToApidocConverter {

    public String convert(String uri, String appName) {

        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "error");

        Apidoc apidoc = new Apidoc();

        Swagger swagger = new SwaggerParser().read(uri);

        // this::swagger.getInfo().getTitle(); would not work on an automated basis.
        // it only works when uploading the first version of an app, when this title is hashed into an id.
        // it's best to determine the app at app creation time, programmatically and then specify it
        // when using this converter.
        apidoc.name = appName;

        Apidoc.Info info = new Apidoc().new Info();

        Apidoc.Contact contact = new Apidoc().new Contact();

        if (swagger.getInfo().getContact() != null) {
            contact.email = swagger.getInfo().getContact().getEmail();
            contact.name = swagger.getInfo().getContact().getName();
            contact.url = swagger.getInfo().getContact().getUrl();
            info.contact = contact;
        }

        Apidoc.License license = new Apidoc().new License();

        if (swagger.getInfo().getLicense() != null) {
            license.name = swagger.getInfo().getLicense().getName();
            license.url = swagger.getInfo().getLicense().getUrl();
            info.license = license;
        }

        apidoc.info = info;
        String basePath = swagger.getBasePath();
        if (basePath != null && basePath.endsWith("/")) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }
        apidoc.base_url = swagger.getHost() + basePath;
        apidoc.description = swagger.getInfo().getDescription();
        apidoc.models = getModels(swagger.getDefinitions());
        apidoc.resources = getResources(swagger.getPaths());

        return gson.toJson(apidoc);
    }

    private Map<String, Apidoc.Resource> getResources(Map<String, Path> paths) {
        Map<String, Apidoc.Resource> resources = new TreeMap<>();

        for (String pathName : paths.keySet()) {

            String root = toSingular(pathName.replaceFirst("/", "").split("/")[0]);
            Apidoc.Resource resource;

            if (resources.get(root) == null) {
                resource = new Apidoc().new Resource();
                resource.operations = new ArrayList<>();
                resources.put(root, resource);
            } else {
                resource = resources.get(root);
            }
            resource.operations.addAll(getOperations(pathName.replaceFirst("/" + pathName.split("/")[1], ""), paths.get(pathName)));
        }

        return resources;
    }

    private String toSingular(String string) {

        return string.endsWith("s") ? string.substring(0, string.length() - 1) : string;

    }

    private List<Apidoc.Operation> getOperations(String pathName, Path path) {

        List<Apidoc.Operation> apidocOperations = new ArrayList<>();

        for (HttpMethod method : path.getOperationMap().keySet()) {

            Operation operation = path.getOperationMap().get(method);

            Apidoc.Operation apidocOperation = new Apidoc().new Operation();

            if (pathName.contains("{")) {
                apidocOperation.path = pathName.replaceAll("\\{", ":").replaceAll("}", "");
            } else {
                apidocOperation.path = pathName;
            }

            apidocOperation.description = operation.getDescription();
            apidocOperation.method = method.name();

            apidocOperation.parameters = getParameters(operation);
            apidocOperation.body = getBody(operation);
            apidocOperation.responses = getResponses(operation);

            if (operation.isDeprecated() != null) {
                if (operation.isDeprecated()) {
                    apidocOperation.deprecation = new Apidoc().new Deprecation("Deprecated");
                }
            }
            apidocOperations.add(apidocOperation);
        }

        return apidocOperations;
    }

    private Map<String, Apidoc.Response> getResponses(Operation operation) {

        Map<String, Apidoc.Response> responses = new TreeMap<>();

        for (String responseName : operation.getResponses().keySet()) {

            Apidoc.Response apidocResponse = new Apidoc().new Response();
            Response response = operation.getResponses().get(responseName);

            apidocResponse.description = response.getDescription();

            RefProperty rp = (RefProperty) response.getSchema();
            if (rp != null) {
                String responseType = rp.getSimpleRef();
                if (typeAliases.get(responseType) != null) {
                    TypeAlias ta = typeAliases.get(responseType);

                    apidocResponse.type = ta.type;
                    apidocResponse.description += "\r\n" + ta.description;
                    apidocResponse.description += ta.format != null ? "\r\n Format: " + ta.format : "";
                } else {
                    apidocResponse.type = rp.getSimpleRef();
                }
            }
            responses.put(responseName, apidocResponse);
        }

        return responses;
    }

    /**
     * Loop for parameters. Skip body parameters
     */
    private List<Apidoc.Parameter> getParameters(Operation operation) {

        List<Apidoc.Parameter> parameters = new ArrayList<>();

        for (Parameter parameter : operation.getParameters()) {

            if (parameter instanceof BodyParameter) {
                continue;
            }

            Apidoc.Parameter apidocParameter = new Apidoc().new Parameter();

            apidocParameter.name = parameter.getName();
            if (parameter instanceof QueryParameter) {
                apidocParameter.type = ((QueryParameter) parameter).getType();
                apidocParameter.location = "query";

            }
            if (parameter instanceof PathParameter) {
                apidocParameter.type = ((PathParameter) parameter).getType();
                apidocParameter.location = "path";
            }
            if (parameter instanceof FormParameter) {
                apidocParameter.type = ((FormParameter) parameter).getType();
                apidocParameter.location = "form";
            }

            apidocParameter.required = parameter.getRequired();
            apidocParameter.description = parameter.getDescription();

            parameters.add(apidocParameter);
        }

        return parameters;
    }

    /**
     * Loop again on parameters in search of a body.
     */
    private Apidoc.Body getBody(Operation operation) {

        Apidoc.Body body = null;

        for (Parameter parameter : operation.getParameters()) {
            if (parameter instanceof BodyParameter) {
                body = new Apidoc().new Body();
                RefModel rm = (RefModel) (((BodyParameter) parameter)).getSchema();
                body.type = rm.getSimpleRef();

            }
        }
        return body;
    }

    class TypeAlias {

        public String name;
        public String type;
        public String description;
        public String example;
        public String format;
    }

    private Map<String, TypeAlias> typeAliases = new HashMap<>();

    private Map<String, Apidoc.Model> getModels(Map<String, Model> definitions) {
        if (definitions == null) {
            return null;
        }
        Map<String, Apidoc.Model> models = new TreeMap<>();
        // scan definitions
        for (String definitionName : definitions.keySet()) {
            Apidoc.Model apidocModel = new Apidoc().new Model();
            Model swaggerDefinition = definitions.get(definitionName);

            apidocModel.description = swaggerDefinition.getDescription();

            String type = ((ModelImpl) swaggerDefinition).getType();

            if ("string".equals(type)) {
                TypeAlias ta = new TypeAlias();

                ta.name = definitionName;
                ta.type = type;
                ta.example = swaggerDefinition.getExample().toString();
                ta.description = swaggerDefinition.getDescription();
                ta.format = ((ModelImpl) swaggerDefinition).getFormat();
                typeAliases.put(definitionName, ta);
                continue;
            }

            Map<String, Property> swaggerProperties = swaggerDefinition.getProperties();

            List<Apidoc.Field> apidocFields = new ArrayList<>();

            for (String swaggerPropertyName : swaggerProperties.keySet()) {

                Property swaggerProperty = swaggerProperties.get(swaggerPropertyName);

                Apidoc.Field field = new Apidoc().new Field();

                field.name = swaggerPropertyName; // swaggerProperty.getName() returns null...
                if ("ref".equals(swaggerProperty.getType())) {
                    TypeAlias ta = typeAliases.get(((RefProperty) swaggerProperty).getSimpleRef());

                    field.type = ta.type;
                    field.description = ta.description;
                    field.example = ta.example != null ? ta.example : "";
                    field.example += ta.format != null ? "\r\n Format: " + ta.format : "";
                    apidocFields.add(field);
                    continue;
                }
                field.type = swaggerProperty.getType();
                field.description = swaggerProperty.getDescription();
                field.required = swaggerProperty.getRequired();
                field.example = swaggerProperty.getExample() != null ? swaggerProperty.getExample().toString() : "";
                field.example += swaggerProperty.getFormat() != null ? "\r\n Format: " + swaggerProperty.getFormat() : "";

                field.attributes = getAttributes(swaggerProperty);
                apidocFields.add(field);
            }

            apidocModel.fields = apidocFields;
            models.put(definitionName, apidocModel);
        }

        return models;
    }

    private List<Apidoc.Attribute> getAttributes(Property swaggerProperty) {
        List<Apidoc.Attribute> attributes = new ArrayList<>();

        Apidoc.Attribute attribute = new Apidoc().new Attribute();
        attribute.name = "swagger";

        Map<String, String> av = new TreeMap<>();

        av.put("format", swaggerProperty.getFormat());
        attribute.value = av;
        attributes.add(attribute);

        return attributes;
    }

    public String getVersion(String uri) throws IOException {

        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "error");

        Swagger swagger = new SwaggerParser().read(uri);

        return swagger.getInfo().getVersion();
    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        OptionParser optionParser = new OptionParser("h");

        optionParser.accepts("swagger").withRequiredArg();
        optionParser.accepts("appkey").withRequiredArg();
        optionParser.accepts("apidoc").withRequiredArg();
        optionParser.accepts("version");
        optionParser.accepts("help");

        OptionSet optionSet = optionParser.parse(args);

        if (optionSet.has("help") || optionSet.has("h")) {
            System.out.println("Usage:");
            System.out.println("-swagger <uri of swagger yaml> -appname <apidoc app key> -apidoc <name of apidoc output file> ");
            System.out.println("or (output to stdout)");
            System.out.println("-swagger <uri of swagger yaml>");
            System.out.println("or (output version of swagger api to stdout)");
            System.out.println(" -version");
            System.exit(0);
        }

        if (!optionSet.has("swagger")) {
            System.out.println("Missing swagger option");
            System.exit(-1);
        }

        URI uri = new URI((String) optionSet.valueOf("swagger"));

        if (uri.getScheme() == null) {
            System.out.println("Bad swagger uri - should be file:// or http:// ");
            System.exit(-1);
        }

        SwaggerToApidocConverter instance = new SwaggerToApidocConverter();

        if (optionSet.has("version")) {
            System.out.print(instance.getVersion((String) optionSet.valueOf("swagger")));
            return;
        }

        if (!optionSet.has("appkey")) {
            System.out.println("Missing appkey option");
            System.exit(-1);
        }

        String data = instance.convert((String) optionSet.valueOf("swagger"), (String) optionSet.valueOf("appkey"));

        if (optionSet.has("apidoc")) {
            Files.write(Paths.get((String) optionSet.valueOf("apidoc")), data.getBytes());
        } else {
            System.out.print(data);
        }
    }

    private Gson gson = new Gson();
}