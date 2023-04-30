package com.wisecoders.util;


import com.wisecoders.FxGitExplorer;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.Window;

public abstract class ButtonDialog$ extends Dialog$<ButtonType> {

    public ButtonDialog$(FxGitExplorer fxGitExplorer) {
        super(fxGitExplorer);
    }

    public ButtonDialog$(FxGitExplorer fxGitExplorer, Modality modality) {
        super(fxGitExplorer, modality );
    }

    public ButtonDialog$(Window window) {
        super( window );
    }

    public ButtonDialog$(Window window, Modality modality) {
        super( window, modality );
    }

    public boolean showDialogIsResultOkDone() {
        showDialog();
        return getResult() != null && getResult().getButtonData() == ButtonBar.ButtonData.OK_DONE;
    }

    public boolean isResultIsOkDone() {
        return getResult() != null && getResult().getButtonData() == ButtonBar.ButtonData.OK_DONE;
    }
}