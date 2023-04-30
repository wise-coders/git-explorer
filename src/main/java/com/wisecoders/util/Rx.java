package com.wisecoders.util;


import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.util.Callback;
import org.controlsfx.control.MaskerPane;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Rx {

    public static final String PRO_EDITION = "e4se";

    public static final String NO_BORDER = "-fx-background-insets: 0;";
    private final Class cls;
    public final Object owner;
    private final Map<String,ResourcesBooleanProperty> flags = new HashMap<>();
    private String suffix = "";
    private final Properties properties = new Properties();
    public static final String SEPARATOR = "separator";

    public Rx(Class cls, Object owner) {
        this.cls = cls;
        this.owner = owner;
        try {
            final InputStream in = cls.getResourceAsStream("resources/" + cls.getSimpleName() + ".properties");
            if ( in != null ) {
                //properties.load( in );
                properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                in.close();
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    public void setSuffix( String suffix ){
        if ( suffix != null ) {
            this.suffix = suffix;
        }
    }

    public Properties getProperties(){
        return properties;
    }

    public String getText( String id ){
        return getString(id + ".text");
    }

    public String getString(String id, String... params ) {
        String val = null;
        if ( suffix != null ){
            val = properties.getProperty( id + "." + suffix );
        }
        if ( val == null ){
            val = properties.getProperty(id);
        }
        if ( val != null ){
            int i = 1;
            for ( String param : params ){
                val = val.replaceAll(":" + i, Matcher.quoteReplacement(param) );
                i++;
            }
        }
        return val;
    }

    public String getStringOrId(String id, String... params) {
        String str = getString( id, params );
        return str == null ? id : str;
    }


    public EventHandler<ActionEvent> getEventHandler(String id) {
        try {
            final Method method = getDeclaredMethodFromClassOrSuperclass(id);
            Action actionAnnotation = method.getAnnotation(Action.class);
            if (actionAnnotation == null) {
                throw new InvalidParameterException("Method '" + id + "' is missing the com.wisecoders.dbs.sys.Action annotation");
            }
            return (ActionEvent event) -> {
                try {
                    Object ret = method.invoke(owner);
                    if ( ret instanceof Task ){
                        executeTask( (Task)ret);
                    }
                } catch (InvocationTargetException ex ) {
                    ex.getTargetException().printStackTrace();
                    showErrorOnOwnerNode( ex.getTargetException() );
                } catch (Throwable ex) {
                    System.out.println("Error executing " + id + " in class " + cls.getName() + ": " + ex.getLocalizedMessage() );
                    showErrorOnOwnerNode( ex.getCause() != null ? ex.getCause() : ex );
                }
            };
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
        }
        return null;
    }


    private Method getDeclaredMethodFromClassOrSuperclass(String id) throws NoSuchMethodException {
        try { return cls.getDeclaredMethod(id);} catch ( NoSuchMethodException ex ){
            Class superCls = cls;
            while ( (superCls = superCls.getSuperclass()) != null ){
                try {
                    return superCls.getDeclaredMethod(id);
                } catch ( NoSuchMethodException ex2 ){}
            }
            throw ex;
        }
    }

    private void showErrorOnOwnerNode(Throwable ex ){
        if ( owner instanceof Dialog){
            showError( ((Dialog)owner).getDialogPane().getScene(), ex );
        } else if ( owner instanceof Scene ){
            showError( ((Scene)owner), ex );
        } else if ( owner instanceof Region ){
            showError(((Region)owner).getScene(), ex );
        }
    }


    public void cancelTasksOfClass( Class cls ){
        for ( Task task : tasks ){
            if ( cls.isAssignableFrom( task.getClass() ) ) {
                task.cancel();
            }
        }
    }

    private final List<Task> tasks = new CopyOnWriteArrayList<>();
    private final ExecutorService service = Executors.newFixedThreadPool(5 );


    private final Button cancelTaskButton = new Button("Cancel");

    public void executeTask(Task task ) {
        executeTask( task, true );
    }

    public void executeTask(Task task, boolean showMaskerPane ){
        if ( task != null ) {
            if (maskerPane != null && showMaskerPane) {
                maskerPane.textProperty().bind(task.messageProperty());
                maskerPane.progressProperty().bind(task.progressProperty());
                Pane node = (Pane) maskerPane.lookup(".masker-center");
                if (node != null) {
                    if (!node.getChildren().contains(cancelTaskButton)) {
                        node.getChildren().add(cancelTaskButton);
                    }
                    cancelTaskButton.setOnAction((event) -> task.cancel(true));
                }
            }
            task.stateProperty().addListener((o, p, n) -> {
                switch (task.getState()) {
                    case READY:
                    case RUNNING:
                    case SCHEDULED:
                        if (maskerPane != null && showMaskerPane) maskerPane.setVisible(true);
                        break;
                    default:
                        tasks.remove(task);
                        if (maskerPane != null) {
                            maskerPane.setVisible(false);
                            maskerPane.textProperty().unbind();
                            maskerPane.progressProperty().unbind();
                            Pane node = (Pane) maskerPane.lookup(".masker-center");
                            if (node != null ) {
                                node.getChildren().remove( cancelTaskButton );
                            }
                        }
                        break;
                }
                fireEvents();
            });
            tasks.add(task);
            service.submit(task);
        }
    }

    public boolean hasTasksOfInstance( Class cls ){
        for ( Task task : tasks ){
            if ( cls.isAssignableFrom( task.getClass() )) return true;
        }
        return false;
    }


    public boolean isFlag( String id ){
        return flags.containsKey( id ) && flags.get( id ).get();
    }

    private void bindFlags(Object node, String id) {
        try {
            final Method method = getDeclaredMethodFromClassOrSuperclass(id);
            final Action actionAnnotation = method.getAnnotation(Action.class);
            if (actionAnnotation == null) {
                throw new InvalidParameterException("Method '" + id + "' is missing the com.wisecoders.dbs.sys.Action annotation");
            } else {
                final String flagEnabled = actionAnnotation.enabledProperty();
                final String flagDisabled = actionAnnotation.disabledProperty();
                final String flagSelected = actionAnnotation.selectedProperty();
                if ( StringUtil.isFilledTrim( actionAnnotation.enabledProperty()) ){
                    final ResourcesBooleanProperty boolProperty = flags.get( flagEnabled );
                    if ( boolProperty == null ) {
                        throw new NullPointerException("Flag not found '" + flagEnabled + "'");
                    }
                    if ( node instanceof MenuItem ) ((MenuItem)node).disableProperty().bind( boolProperty.not() );
                    else if ( node instanceof Node ) ((Node)node).disableProperty().bind( boolProperty.not() );
                } else if ( StringUtil.isFilledTrim( actionAnnotation.disabledProperty()) ){
                    final ResourcesBooleanProperty boolProperty = flags.get( flagDisabled );
                    if ( boolProperty == null ) {
                        throw new NullPointerException("Flag not found '" + flagDisabled + "'");
                    }
                }
                if ( StringUtil.isFilledTrim( flagSelected ) ){
                    final ResourcesBooleanProperty boolProperty = flags.get( flagSelected );
                    if ( boolProperty == null ) {
                        throw new NullPointerException("Flag not found '" + flagSelected + "'");
                    }
                    if ( node instanceof CheckMenuItem )((CheckMenuItem)node).selectedProperty().bindBidirectional( boolProperty );
                    else if ( node instanceof RadioMenuItem )((RadioMenuItem)node).selectedProperty().bindBidirectional( boolProperty );
                    else if ( node instanceof ToggleButton )((ToggleButton)node).selectedProperty().bindBidirectional( boolProperty );
                }
            }
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
        }
    }

    public void bindDialogOkFlag( Object node ){
        if ( flags.containsKey("flagDialogOk") &&  node instanceof Node ) {
            ((Node)node).disableProperty().bind( flags.get( "flagDialogOk" ).not() );
        }
    }

    public static final HashMap<String,Image> cachedImages = new HashMap<>();

    public static Image getImage(String name ){
        if ( cachedImages.containsKey( name )) return cachedImages.get( name );
        final Image image = new Image("/icons/" + name);
        cachedImages.put( name, image );
        return image;
    }


    public void fireEvents(){
        if ( initiator != null ){
            initiator.evaluate();
        }
        for ( String key : flags.keySet()){
            flags.get(key ).evaluate();
        }
    }

    private ResourcesFlagCallback initiator;

    public void addInitiator( ResourcesFlagCallback callback ){
        this.initiator = callback;
    }

    public BooleanProperty addFlag(String name, ResourcesFlagCallback evaluator ){
        if ( flags.containsKey( name )){
            throw new NullPointerException("Event '" + name + "' already defined.");
        }
        ResourcesBooleanProperty booleanProperty = new ResourcesBooleanProperty( evaluator );
        flags.put( name, booleanProperty );
        fireEvents();
        return booleanProperty;
    }

    public static class ResourcesBooleanProperty extends SimpleBooleanProperty{
        private final ResourcesFlagCallback callback;

        public ResourcesBooleanProperty(ResourcesFlagCallback callback){
            this.callback = callback;

        }
        public void evaluate(){
            boolean actual = callback.evaluate();
            if ( actual != getValue() ){
                setValue( actual );
            }
        }
    }



    public Label label(String id, Node node) {
        Label label = label( id );
        label.setLabelFor( node );
        return label;
    }

    public Label label(String id) {
        String text = getString(id + ".text");
        final Label label = new Label( text );
        label.setMnemonicParsing( true );
        label.setGraphic( getGlyph( id ));
        configureCss( label, id );
        configureTipIconFromResourceId( label, id );
        return label;
    }

    private Separator createGrowSeparator(){
        final Separator separator = new Separator();
        HBox.setHgrow( separator, Priority.ALWAYS );
        return separator;
    }


    public Button button(String id){
        return button( id, true );
    }

    public Button noTextButton(String id) {
        return button(id, false);
    }

    public ToggleButton toggleButton(String id) {
        return toggleButton( id, false);
    }
    public ToggleButton toggleButton(String id, boolean selected) {
        final ToggleButton button = new ToggleButton(getActionText(id));
        button.setSelected( selected);
        configureButton( id, button );
        return button;
    }

    private Button button(String id, boolean setText) {
        final Button button = setText ? new Button(getActionText(id)) : new Button();
        configureButton( id, button );
        return button;
    }

    private void configureTipIconFromResourceId(Control node, String id ) {
        String tip = getString(id + ".tooltip");
        configureTipIconUsingText(node, tip);
        if ( getString(id + ".mandatory") != null ){
            node.getStyleClass().add("mandatory-icon");
        }
    }

    public static void configureTipIconUsingText(Control node, String tip) {
        if ( tip != null ) {
            node.getStyleClass().add("tooltip-icon");
            final Tooltip tooltip = new Tooltip();
            if ( tip.toLowerCase().startsWith("<html>")) {
                Label tooltipLabel = new Label(tip);
                tooltipLabel.setMinWidth(Region.USE_PREF_SIZE);
                tooltip.setGraphic(tooltipLabel);
                tooltip.setContentDisplay(ContentDisplay.BOTTOM);
            } else {
                tooltip.setText(tip);
            }
            tooltip.setHideOnEscape(true);
            node.setOnMouseEntered((e) -> {
                Bounds screenBounds = node.localToScreen(node.getBoundsInLocal());
                tooltip.show(node,
                        screenBounds.getMinX(),
                        screenBounds.getMaxY() + 15);
            });
            node.setOnMouseExited((e) -> tooltip.hide());
        }
    }


    private void configureTooltip(Control node, String id ){
        String tip = getString(id + ".tooltip");
        if( tip != null ){
            final Tooltip tooltip = new Tooltip();
            if ( tip.toLowerCase().startsWith("<html>")) {
                tooltip.setContentDisplay(ContentDisplay.BOTTOM);
                tooltip.setGraphic(new Label(tip));
            } else {
                tooltip.setText( tip );
            }
            node.setTooltip(tooltip);
        }
    }

    public List<Button> buttons(String... ids ){
        final List<Button> items = new ArrayList<Button>();
        for ( String id : ids ){
            items.add(button(id));
        }
        return items;
    }


    public MenuButton createMenuButton( String id, boolean setAction ) {
        final MenuButton button = new MenuButton();
        button.setPadding( new Insets( 0,0,0,0));
        button.setText( getActionText(id));
        button.setOnAction( setAction ? getEventHandler(id) : event -> button.show() );
        button.setGraphic( getGlyph( id ));
        configureTooltip( button, id );
        //fixGlyph( button, id );
        if ( setAction) {
            bindFlags( button, id );
        }
        configureCss( button, id );
        return button;
    }

    public SplitMenuButton createSplitMenuButton( String id, boolean setAction ) {
        final SplitMenuButton button = new SplitMenuButton();
        button.setText( getActionText(id));
        button.setOnAction( setAction ? getEventHandler(id) : event -> button.show() );
        configureTooltip( button, id );
        button.setGraphic( getGlyph( id ));
        if ( setAction) {
            bindFlags( button, id );
        }
        configureCss( button, id );
        return button;
    }


    public ButtonBase configureButton( String id, ButtonBase button ){
        button.setOnAction(getEventHandler(id));
        button.setGraphic( getGlyph( id ));
        button.setMnemonicParsing( true );
        configureTooltip( button, id );
        configureCss( button, id );
        bindFlags( button, id );
        return button;
    }


    private Node configureCss( Node node, String id ){
        String cssClass = getString(id + ".cssClass");
        if ( cssClass != null ) {
            node.getStyleClass().addAll(cssClass.split(","));
        }
        return node;
    }


    public Node getGlyph( String id ){
        String key;
        if ( ( key = getString( id + ".image" )) != null ){
            try {
                return getImageView(key);
            } catch ( IllegalArgumentException ex ){
            }
        }
        return null;
    }

    public static ImageView getImageView(String key) {
        final Image image = getImage(key);
        ImageView imageView = new ImageView(image);
        imageView.getStyleClass().add("transparent-on-dark-image");
        return imageView;
    }


    public TextField textField(String id ){
        TextField textField = new TextField();
        String promptText = getString(id + ".promptText");
        configureTipIconFromResourceId(textField, id );
        configureCss( textField, id );
        if ( promptText != null ) {
            textField.setPromptText( promptText );
        }
        return textField;
    }


    public CheckBox checkBox(String id, boolean selected ) {
        CheckBox check = checkBox( id );
        check.setSelected( selected);
        return check;
    }

    public CheckBox checkBox(String id ){
        CheckBox checkBox = new CheckBox( getString(id + ".text") + WINDOWS_SCALE_125_ISSUE );
        checkBox.setMnemonicParsing( true );
        checkBox.textOverrunProperty().setValue(OverrunStyle.CLIP);
        configureCss( checkBox, id );
        configureTipIconFromResourceId( checkBox, id );
        return checkBox;
    }

    public RadioButton radioButton(String id, boolean selected ) {
        RadioButton radio = radioButton( id );
        radio.setSelected( selected);
        return radio;
    }

    public RadioButton radioButton(String id ){
        RadioButton radio = new RadioButton( getString(id + ".text") + WINDOWS_SCALE_125_ISSUE );
        radio.setMnemonicParsing( true );
        radio.textOverrunProperty().set(OverrunStyle.CLIP);
        configureCss( radio, id );
        configureTipIconFromResourceId( radio, id );
        return radio;
    }



    private static final String WINDOWS_SCALE_125_ISSUE = "  ";

    public CheckMenuItem checkMenuItem(String id) {
        fireEvents();
        String text = getString(id + ".text");
        CheckMenuItem checkMenuItem = new CheckMenuItem(text);
        checkMenuItem.setOnAction( getEventHandler(id) );
        bindFlags( checkMenuItem, id );
        return checkMenuItem;
    }

    public List<MenuItem> menuItems(String... ids ){
        List<MenuItem> items = new ArrayList<MenuItem>();
        for ( String id : ids ){
            if ( SEPARATOR.equals( id )){
                items.add( new SeparatorMenuItem());
            } else {
                items.add(menuItem(id));
            }
        }
        return items;
    }

    public MenuItem menuItem(String id) {
        final MenuItem item = new MenuItem( getActionText( id ), getGlyph( id ));
        item.setOnAction( getEventHandler(id) );
        bindFlags( item, id );
        return item;
    }


    public MenuItem informationMenuItem(String id){
        final MenuItem item = new MenuItem( getStringOrId( id ), getGlyph( id ));
        item.setStyle("-fx-font-style: italic;");
        return item;
    }


    private String getActionText( String id ){
        return getString(id + ".text");
    }

    private MaskerPane maskerPane;
    public void setMaskerPane( MaskerPane maskerPane ){
        this.maskerPane = maskerPane;
    }

    public void showInformation( Scene scene, String idOrMessage, String... params ) {
        createAlert( scene, idOrMessage, Alert.AlertType.INFORMATION, params ).showAndWait();
    }

    public Alert$ showOptionsDialog( Scene scene, String idOrMessage, String... parameters ){
        final Alert$ alert = createAlert( scene, idOrMessage, Alert.AlertType.CONFIRMATION, parameters );
        String btnText;
        alert.getButtonTypes().clear();
        if ( ( btnText = getString( idOrMessage + ".yes")) != null ){
            alert.getButtonTypes().add(ButtonType.YES);
            ((Button)alert.getDialogPane().lookupButton( ButtonType.YES )).setText( btnText );
        }
        if ( ( btnText = getString( idOrMessage + ".no")) != null ){
            alert.getButtonTypes().add(ButtonType.NO);
            ((Button)alert.getDialogPane().lookupButton( ButtonType.NO )).setText( btnText );
        }
        if ( ( btnText = getString( idOrMessage + ".apply")) != null ){
            alert.getButtonTypes().add(ButtonType.APPLY);
            ((Button)alert.getDialogPane().lookupButton( ButtonType.APPLY )).setText( btnText );
        }
        if ( ( btnText = getString( idOrMessage + ".close")) != null ){
            alert.getButtonTypes().add(ButtonType.CLOSE);
            ((Button)alert.getDialogPane().lookupButton( ButtonType.CLOSE )).setText( btnText );
        }
        if ( ( btnText = getString( idOrMessage + ".cancel")) != null ){
            alert.getButtonTypes().add(ButtonType.CANCEL);
            ((Button)alert.getDialogPane().lookupButton( ButtonType.CANCEL )).setText( btnText );
        }
        if ( alert.getButtonTypes().isEmpty() ){
            alert.getButtonTypes().add(ButtonType.CLOSE);
        }
        return alert;
    }

    public static final Pattern PATTERN_HEADING2_MESSAGE = Pattern.compile("<html><h2>(.*)</h2>(.*)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );

    public Alert$ createAlert( Scene window, String id, Alert.AlertType type, String... params ) {
        return createAlert( window, id, type, null, params );
    }
    public Alert$ createAlert( Scene scene, String id, Alert.AlertType type, Throwable cause, String... params ) {
        String content = getString(id + ".content", params );
        if ( getString(id + ".header") == null && content == null ) content = id;
        String title = getString(id + ".title");
        if ( title == null ){
            switch ( type ){
                case CONFIRMATION: title = "Confirmation Dialog"; break;
                case ERROR: title = "Error Dialog"; break;
                case INFORMATION: title = "Information Dialog"; break;
                case WARNING: title = "Warning Dialog"; break;
            }
        }
        String headerText = getString(id + ".header");
        Matcher matcher;
        if ( content != null && ( matcher = PATTERN_HEADING2_MESSAGE.matcher( content)).matches() ){
            headerText = matcher.group( 1 );
            content = "<html>" + matcher.group( 2 );
        }
        return new Alert$(type).
                withOwner( scene ).
                withTitle(title).
                withHeaderText( headerText ).
                withContextText( content ).
                withException( cause );
    }

    public Dialog prepareDialog( Dialog dialog, String id){
        dialog.setTitle( getString(id + ".title"));
        dialog.setHeaderText( getString( id + ".header"));
        dialog.setContentText( getString( id + ".content"));
        return dialog;
    }
    public String showInputString(Scene owner, String id ) {
        return showInputString( owner, id, null );
    }
    public String showInputString(Scene owner, String id, String defo ) {
        final TextInputDialog inputDialog = new TextInputDialog( defo );
        if ( owner != null ) {
            inputDialog.initOwner(owner.getWindow());
        }
        prepareDialog( inputDialog, id );
        String title = getString(id + ".title");
        inputDialog.setTitle( title != null ? title : "Input Dialog");
        String header = getString(id + ".header");
        String content = getString(id + ".content");
        if ( header == null && content == null ) header = id;
        inputDialog.setHeaderText(header);
        inputDialog.setContentText(content);
        Optional<String> optional = inputDialog.showAndWait();
        return optional.isPresent() ? optional.get() : null;
    }

    public void showError( Scene owner, Throwable ex ) {
        String message = ex.getLocalizedMessage();
        if ( message == null ) message = ex.toString();
        showError( owner, message, ex );
    }

    public void showError( Scene scene, String idOrMessage ) {
        showError( scene, idOrMessage, null);
    }

    public void showError( Scene owner, String idOrMessage, Throwable ex ) {
        showError( owner, idOrMessage, ex, null, null );
    }

    public void showError(Scene owner, String idOrMessage, Throwable ex, String helpButtonText, Callback helpAction){
        String content = getString(idOrMessage + ".content");
        if ( content == null && ex != null ) content = ex.getLocalizedMessage();
        if ( content == null ) content = idOrMessage;
        String title = getString(idOrMessage + ".title");
        if ( title == null ) title = "Error Dialog";
        final Alert$ alert = new Alert$(Alert.AlertType.ERROR ).
                withTitle(title).
                withOwner( owner ).
                withHeaderText(getString(idOrMessage + ".header") ).
                withContextText( content ).
                withException( ex );
        if ( helpButtonText != null && helpAction != null ) {
            alert.withHelpButton( helpButtonText, helpAction);
        }

        // LONG TEXT EXCEPTIONS IN A SINGLE LINE MAKE DIALOG SHOW MUCH LEFT ON SCREEN
        Platform.runLater(()-> { if ( owner != null && owner.getWindow() != null ) {
            alert.setX( owner.getWindow().getX() + (owner.getWindow().getWidth()/2d) - (Math.min(600d,alert.getWidth())/2d) ); } });
        alert.showAndWait();
    }


    public static void setTableViewColumnWidths( TableView tableView, double... widths ){
        for ( int i = 0; i < widths.length && i < tableView.getColumns().size(); i++ ){
            ((TableColumn)tableView.getColumns().get(i)).prefWidthProperty().bind(tableView.widthProperty().multiply(widths[i]));;
        }
    }


    public enum ScreenResolution { Low, Medium, Large };

    private static ScreenResolution screenResolution = ScreenResolution.Large;

    static {
        try {
            Rectangle2D primary = Screen.getPrimary().getVisualBounds();
            double outputScaleY = Math.max(0.1d, Screen.getPrimary().getOutputScaleY());
            double factor = primary.getHeight() / outputScaleY ;
            if ( factor < 800d ) {
                screenResolution = ScreenResolution.Low;
            } else if ( factor < 1000d ){
                screenResolution = ScreenResolution.Medium;
            }
        } catch ( Throwable ex ){}
    }


    public static double getSizeFactor(){
        return screenResolution != ScreenResolution.Large ? 0.75d : 1d;
    }


    public static void runAndWait(Runnable action) {
        if (action != null) {
            if (Platform.isFxApplicationThread()) {
                try {
                    action.run();
                } catch ( Throwable ex ){
                    System.out.println("Error in FxUtil.runAndWait()");
                    ex.printStackTrace();
                }
            } else {
                final CountDownLatch doneLatch = new CountDownLatch(1);
                Platform.runLater(() -> {
                    try {
                        action.run();
                    } catch ( Throwable ex ){
                        System.out.println("Error in FxUtil.runAndWait()");
                        ex.printStackTrace();
                    } finally {
                        doneLatch.countDown();
                    }
                });
                try {
                    doneLatch.await();
                } catch (InterruptedException e) {
                    // ignore exception
                }
            }
        }
    }


}

