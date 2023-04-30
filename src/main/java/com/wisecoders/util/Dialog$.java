package com.wisecoders.util;


import com.wisecoders.FxGitExplorer;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafx.util.Callback;
import org.controlsfx.control.MaskerPane;
import org.controlsfx.control.Notifications;

import java.util.Optional;

public abstract class Dialog$<T> extends Dialog<T> {


    public final Rx rx;
    public final MaskerPane maskerPane = new MaskerPane();
    private Node initFocusedNode;

    private boolean skipShowingDialog;

    public Dialog$( Scene workspaceWindow ) {
        this( workspaceWindow, Modality.WINDOW_MODAL );
    }

    public Dialog$( Scene workspaceWindow, Modality modality ) {
        this( modality );
        initOwner( workspaceWindow );
    }
    public Dialog$( Window window ) {
        this( window, Modality.WINDOW_MODAL );
    }

    public Dialog$( Window window, Modality modality ) {
        this( modality);
        if ( window != null ) {
            initOwner( window );
        }
    }

    private boolean showAltTip = true;
    public Dialog$( Modality modality ) {
        initModality(modality);
        maskerPane.setVisible( false );
        rx = new Rx( getClass(), this );
        setDialogTitleAndHeader();
        rx.setMaskerPane( maskerPane );
        setResizable( true );
    }


    public void setDialogTitleAndHeader(){
        setDialogTitle( rx.getString("dialog.title") );
        setHeaderText( rx.getString("dialog.header"));
    }
    private boolean dialogLogged = false;
    public void setDialogTitle( String title ){
        if ( title != null ) {
            setTitle(title);
            if ( !dialogLogged ){
                dialogLogged = true;
            }
        }
    }

    public void requestFocusOn( Node node ){
        Platform.runLater( node::requestFocus );
    }

    public abstract Node createContentPane();

    public abstract void createButtons();

    public Button createCancelButton(){
        getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        Button btn = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
        String text = rx.getString( "cancel.text");
        if( text != null ) btn.setText( text );
        btn.addEventFilter(ActionEvent.ACTION, (event) -> {
            if (!cancel()) {
                event.consume();
            }
        });
        return btn;
    }
    public Button createCloseButton(){
        if ( !getDialogPane().getButtonTypes().contains(ButtonType.CLOSE)) {
            getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        }
        Button btn = (Button) getDialogPane().lookupButton(ButtonType.CLOSE);
        String text = rx.getString( "close.text");
        if( text != null ) btn.setText( text );
        btn.addEventFilter(ActionEvent.ACTION, (event) -> {
            if (!cancel()) {
                event.consume();
            }
        });
        return btn;
    }
    public Button createOkButton(){
        getDialogPane().getButtonTypes().add(ButtonType.OK);
        Button btn = (Button) getDialogPane().lookupButton(ButtonType.OK);
        String text = rx.getString( "ok.text");
        if( text != null ) btn.setText( text );
        btn.addEventFilter(ActionEvent.ACTION, (event) -> {
            if (!apply()) {
                event.consume();
            }
        });
        return btn;
    }

    public Button createActionButton(String id) {
        return createActionButton( id, ButtonBar.ButtonData.OK_DONE );
    }
    public Button createActionButton(String id, ButtonBar.ButtonData buttonData){
        final ButtonType buttonType = new ButtonType( id, buttonData );
        getDialogPane().getButtonTypes().add(buttonType);
        return createActionButton( id, (Button) getDialogPane().lookupButton(buttonType));
    }

    public Button createActionButton(String id, ButtonType buttonType){
        getDialogPane().getButtonTypes().add(buttonType);
        return createActionButton( id, (Button) getDialogPane().lookupButton(buttonType));
    }

    private Button createActionButton(String id, Button btn){
        btn.setText( rx.getStringOrId( id + ".text"));
        rx.configureButton( id, btn );
        btn.addEventFilter(ActionEvent.ACTION, (event) -> {
            EventHandler<ActionEvent> handler = rx.getEventHandler( id );
            handler.handle( event );
            event.consume();
        });
        return btn;
    }

    public void setInitFocusedNode(Node node ){
        this.initFocusedNode = node;
    }

    static final ButtonType HELP_BUTTON_TYPE = new ButtonType("Help", ButtonBar.ButtonData.LEFT );
    static final ButtonType SUPPORT_BUTTON_TYPE = new ButtonType("Support", ButtonBar.ButtonData.LEFT );


    public Optional<T> showDialog(FxGitExplorer fxGitExplorer) {
            centerOn(fxGitExplorer);
        return showDialog();
    }

    public Optional<T> showDialog(Window window ) {
        centerOn( window.getScene() );
        return showDialog();
    }

    public Optional<T> showDialog(Node parentNode ) {
        if (parentNode != null) {
            centerOn(parentNode.getScene());
        }
        return showDialog();
    }

    public Optional<T> showDialog(){
        if (skipShowingDialog){
            return Optional.empty();
        }
        final Node content = createContentPane();
        if ( content != null ){
            createButtons();
            final StackPane stackPane = new StackPane();
            stackPane.getChildren().addAll( content, maskerPane );
            getDialogPane().setContent(stackPane);
            getDialogPane().setMinHeight(Region.USE_COMPUTED_SIZE);
        }
        try {
            if ( initFocusedNode != null ) {
                Platform.runLater( ()->{
                    initFocusedNode.requestFocus();
                    if ( initFocusedNode instanceof ComboBox) {
                        ((ComboBox) initFocusedNode).getEditor().selectAll();
                    }
                });
            }
            return super.showAndWait();
        } catch ( Throwable ex ){
            ex.printStackTrace();
        }
        return Optional.empty();
    }

    public abstract boolean apply();
    public boolean cancel(){ return true; }

    private void centerOn( Scene primaryScene ){
        setX(primaryScene.getX() - (getWidth() / 2));
        setY(primaryScene.getY() - (getHeight() / 2));
    }

    protected void showAlert( String id ){
        Alert$ alert = new Alert$().withContextText(rx.getString(id + ".Alert.text")).withOwner( getDialogPane().getScene() );
        String title = rx.getString( id + ".Alert.title");
        if ( title != null ) alert.withTitle( title );
        alert.showAndWait();
    }

    public void showNotificationPane( String id ){
        final String text = rx.getString(id + ".text");
        Notifications.create().owner( getDialogPane().getScene().getWindow() ).
                text( text == null ? id : text ).
                position(Pos.BOTTOM_LEFT ).
                show();
    }

    public Scene getDialogScene() {
        return getDialogPane().getScene();
    }

    public void showError( String idOrMessage ){
        rx.showError( getDialogScene(), idOrMessage );
    }
    public void showError( String idOrMessage, Throwable cause ){
        rx.showError( getDialogScene(), idOrMessage, cause );
    }
    public void showError( String idOrMessage, Throwable cause, String helpButtonText, Callback action ){
        rx.showError( getDialogScene(), idOrMessage, cause, helpButtonText, action );
    }

    public void showInformation( String idOrMessage, String... params ){
        rx.showInformation( getDialogScene(), idOrMessage, params );
    }

    public String showInputString(String idOrMessage ){
        return rx.showInputString( getDialogScene(), idOrMessage );
    }
    public String showInputString(String idOrMessage, String defo ){
        return rx.showInputString( getDialogScene(), idOrMessage, defo );
    }

    public Alert$ showOptionsDialog(String idOrMessage ) {
        return rx.showOptionsDialog( getDialogScene(), idOrMessage);
    }


    public static void setRegionPrefHeight( Region region, double height ){
        region.setPrefHeight( height * Rx.getSizeFactor() );
    }

    public static void setRegionPrefWidth( Region region, double width ){
        region.setPrefWidth( width * Rx.getSizeFactor() );
    }

    public static void setRegionPrefSize( Region region, double width, double height ){
        region.setPrefSize( width * Rx.getSizeFactor(), height * Rx.getSizeFactor() );
    }

    public void initOwner( Scene workspaceWindow){
            initOwner(  workspaceWindow.getWindow() );
    }

    public static void setManagedVisible(Node... nodes ){
        for ( Node node : nodes) {
            node.managedProperty().bind(node.visibleProperty());
        }
    }

    public void skipShowingDialog(){
        this.skipShowingDialog = true;
    }


    public Rx getRx(){
        return rx;
    }


}


