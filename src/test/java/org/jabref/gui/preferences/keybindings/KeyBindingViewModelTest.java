package org.jabref.gui.preferences.keybindings;

import java.util.Optional;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.DialogService;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.preferences.CliPreferences;

import com.airhacks.afterburner.injection.Injector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KeyBindingViewModelTest {

    @Test
    void resetToDefault() {
        // Set new key binding
        KeyBindingRepository keyBindingRepository = new KeyBindingRepository();
        GuiPreferences preferences = mock(GuiPreferences.class);
        when(preferences.getKeyBindingRepository()).thenReturn(keyBindingRepository);
        Injector.setModelOrService(CliPreferences.class, preferences);

        KeyBindingsTabViewModel keyBindingsTabViewModel = new KeyBindingsTabViewModel(keyBindingRepository, mock(DialogService.class), preferences);
        KeyBinding binding = KeyBinding.ABBREVIATE;

        KeyBindingViewModel viewModel = new KeyBindingViewModel(keyBindingRepository, binding, binding.getDefaultKeyBinding());
        keyBindingsTabViewModel.selectedKeyBindingProperty().set(Optional.of(viewModel));

        KeyEvent shortcutKeyEvent = new KeyEvent(KeyEvent.KEY_PRESSED, "F1", "F1", KeyCode.F1, true, false, false,
                false);

        assertFalse(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.ABBREVIATE, shortcutKeyEvent));

        keyBindingsTabViewModel.setNewBindingForCurrent(shortcutKeyEvent);
        keyBindingsTabViewModel.storeSettings();

        assertTrue(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.ABBREVIATE, shortcutKeyEvent));

        // Reset to default
        viewModel.resetToDefault();

        assertFalse(keyBindingRepository.checkKeyCombinationEquality(KeyBinding.ABBREVIATE, shortcutKeyEvent));
    }
}
