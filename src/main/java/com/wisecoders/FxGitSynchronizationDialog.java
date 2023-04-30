package com.wisecoders;

import com.wisecoders.util.Dialog$;
import com.wisecoders.util.GridPane$;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.io.File;


public class FxGitSynchronizationDialog extends Dialog$<String> {

    private final TextArea leftTextArea = new TextArea();
    private final TextArea middleTextArea = new TextArea();
    private final TextArea rightTextArea = new TextArea();
    private final String leftTitle, rightTitle;

    public FxGitSynchronizationDialog(FxGitExplorer fxGitExplorer, String leftTitle, String left, String rightTitle, String right ){
        super(fxGitExplorer);
        leftTextArea.setText( left );
        middleTextArea.setText( right );
        rightTextArea.setText( right);
        this.leftTitle = leftTitle;
        this.rightTitle = rightTitle;
    }

    @Override
    public Node createContentPane() {
        GridPane$ pane = new GridPane$().withGap().withRows(-2,-1);
        pane.add( new Label(leftTitle), "0,0,l,c");
        pane.add( new Label("Result"), "1,0,l,c");
        pane.add( new Label(rightTitle), "2,0,l,c");
        pane.add( leftTextArea, "0,1,f,c");
        pane.add( middleTextArea, "1,1,f,c");
        pane.add( rightTextArea, "2,1,f,c");
        return pane;
    }

    @Override
    public void createButtons() {
        createOkButton();
    }

    @Override
    public boolean apply() {
        setResult( middleTextArea.getText());
        return false;
    }
}
