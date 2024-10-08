package org.jabref.gui.fieldeditors;

import javax.swing.undo.UndoManager;

import javafx.scene.control.Button;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.mergeentries.FetchAndMergeEntry;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

public class ISSNEditorViewModel extends AbstractEditorViewModel {
    private final TaskExecutor taskExecutor;
    private final DialogService dialogService;
    private final UndoManager undoManager;
    private final StateManager stateManager;
    private final GuiPreferences preferences;

    public ISSNEditorViewModel(
            Field field,
            SuggestionProvider<?> suggestionProvider,
            FieldCheckers fieldCheckers,
            TaskExecutor taskExecutor,
            DialogService dialogService,
            UndoManager undoManager,
            StateManager stateManager,
            GuiPreferences preferences) {
        super(field, suggestionProvider, fieldCheckers, undoManager);
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;
        this.undoManager = undoManager;
        this.stateManager = stateManager;
        this.preferences = preferences;
    }

    public void showJournalInfo(Button journalInfoButton) {
        PopOverUtil.showJournalInfo(journalInfoButton, entry, dialogService, taskExecutor);
    }

    public void fetchBibliographyInformation(BibEntry bibEntry) {
        stateManager.getActiveDatabase().ifPresentOrElse(
                databaseContext -> new FetchAndMergeEntry(databaseContext, taskExecutor, preferences, dialogService, undoManager)
                        .fetchAndMerge(bibEntry, StandardField.ISSN),
                () -> dialogService.notify(Localization.lang("No library selected"))
        );
    }
}
