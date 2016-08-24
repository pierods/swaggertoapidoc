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

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by piero on 4/28/16.
 */
public class SwaggerToApidocConverterTest {
    @Test
    public void convert() throws Exception {

        ObjectMapper om = new ObjectMapper();

        Map<String, Object> om1 = om.readValue(Files.readAllBytes(Paths.get(new java.io.File(".").getCanonicalPath(), "src/test/resources/minimal.json")), Map.class);
        Map<String, Object> om2 = om.readValue(new SwaggerToApidocConverter().convert("file://" + Paths.get(new java.io.File(".").getCanonicalPath(), "src/test/resources/minimal.yaml"), "testapp"), Map.class);

        assertEquals(om1, om2);
    }

    @Test
    public void getVersion() throws Exception {

        assertEquals("0.0.0", new SwaggerToApidocConverter().getVersion("file://" + Paths.get(new java.io.File(".").getCanonicalPath(), "src/test/resources/minimal.yaml")));

    }

}