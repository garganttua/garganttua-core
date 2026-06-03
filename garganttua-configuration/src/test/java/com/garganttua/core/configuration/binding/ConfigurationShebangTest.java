package com.garganttua.core.configuration.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

class ConfigurationShebangTest {

    @Test
    void yamlShebangFirstLineIsRead() {
        String yaml = "#!injection\nwithPackage: com.example\nautoDetect: true\n";
        assertEquals(Optional.of("injection"), ConfigurationShebang.extractFromText(yaml));
    }

    @Test
    void shebangToleratesGarganttuaPrefix() {
        assertEquals(Optional.of("mutex"),
                ConfigurationShebang.extractFromText("#!garganttua:mutex\nfoo: bar"));
    }

    @Test
    void shebangIsReadAfterLeadingBlankLines() {
        assertEquals(Optional.of("expression"),
                ConfigurationShebang.extractFromText("\n\n   \n#!expression\nkey: value"));
    }

    @Test
    void jsonReservedModuleKeyIsRead() {
        String json = "{\n  \"$module\": \"injection\",\n  \"withPackage\": \"com.example\"\n}";
        assertEquals(Optional.of("injection"), ConfigurationShebang.extractFromText(json));
    }

    @Test
    void xmlProcessingInstructionIsRead() {
        String xml = "<?garganttua module=\"runtime\"?>\n<config><withPackage>com.x</withPackage></config>";
        assertEquals(Optional.of("runtime"), ConfigurationShebang.extractFromText(xml));
    }

    @Test
    void xmlRootAttributeIsRead() {
        String xml = "<config module=\"workflow\">\n  <stage>a</stage>\n</config>";
        assertEquals(Optional.of("workflow"), ConfigurationShebang.extractFromText(xml));
    }

    @Test
    void aliasWithDotsAndDashesIsAccepted() {
        assertEquals(Optional.of("my-module.sub"),
                ConfigurationShebang.extractFromText("#!my-module.sub\n"));
    }

    @Test
    void noShebangYieldsEmpty() {
        assertTrue(ConfigurationShebang.extractFromText("withPackage: com.example\nautoDetect: true").isEmpty());
        assertTrue(ConfigurationShebang.extractFromText("").isEmpty());
        assertTrue(ConfigurationShebang.extractFromText(null).isEmpty());
    }

    @Test
    void plainHashCommentIsNotAShebang() {
        // a normal YAML comment (single #) must not be mistaken for a target alias
        assertTrue(ConfigurationShebang.extractFromText("# just a comment\nkey: value").isEmpty());
    }
}
