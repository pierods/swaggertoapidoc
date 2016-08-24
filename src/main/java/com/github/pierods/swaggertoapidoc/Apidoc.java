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

import java.util.List;
import java.util.Map;


/**
 * Created by piero on 4/20/16.
 */
public class Apidoc {

    public String name;
    public ApidocVersion apidoc = new ApidocVersion();
    public Info info;
    public String base_url;
    public String description;
    public Map<String, Model> models;
    public Map<String, Resource> resources;

    class Attribute {
        String name;
        Map<String, String> value;
    }

    class Field {
        String name;
        String type;
        String description;
        Boolean required;
        String defaultValue; // will have to remap
        String example;
        Long minimum; // long would give me a default value of 0
        Long maximum;
        List<Attribute> attributes;
        Deprecation deprecation;
    }

    class Model {
        String description;
        String plural;
        List<Field> fields;
    }

    class Resource {
        String path;
        String description;
        List<Operation> operations;
        List<Attribute> attributes;
        Deprecation deprecation;
    }

    class ApidocVersion {
        public String version = "0.11.23";
    }

    class Info {
        public Contact contact;
        public License license;
    }

    class Contact {
        public String name;
        public String email;
        public String url;
    }

    class License {
        public String name;
        public String url;
    }

    class Operation {
        String method;
        String path;
        String description;
        Body body;
        Deprecation deprecation;
        List<Parameter> parameters;
        Map<String, Response> responses; // key is response code


    }

    class Body {
        String type;
        String description;
        List<Attribute> attributes;
        Deprecation deprecation;

    }

    class Deprecation {
        public Deprecation(String description) {
            this.description = description;
        }
        String description;
    }

    class Parameter {
        String name;
        String type;
        String location;
        String description;
        Deprecation deprecation;
        boolean required;
        String defaultValue; // will have to remap
        String example;
        Long minimum;
        Long maximum;
    }

    class Response {
        String type;
        String description;
        Deprecation deprecation;
    }
}