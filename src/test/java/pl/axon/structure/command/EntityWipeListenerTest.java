package pl.axon.structure.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityWipeListenerTest {

    @Test
    void recognisesEntityWipingCommands() {
        assertTrue(EntityWipeListener.wipesEntities("/killall all"));
        assertTrue(EntityWipeListener.wipesEntities("/KILLALL"));
        assertTrue(EntityWipeListener.wipesEntities("/essentials:killall all"));
        assertTrue(EntityWipeListener.wipesEntities("remove all 200"));
        assertTrue(EntityWipeListener.wipesEntities("/minecraft:kill @e[type=item_display]"));
    }

    @Test
    void ignoresOtherCommands() {
        assertFalse(EntityWipeListener.wipesEntities("/kill"));
        assertFalse(EntityWipeListener.wipesEntities("/kill CallMeTomix"));
        assertFalse(EntityWipeListener.wipesEntities("/kilall all"));
        assertFalse(EntityWipeListener.wipesEntities("/struktury meble"));
        assertFalse(EntityWipeListener.wipesEntities("/killer"));
    }
}
