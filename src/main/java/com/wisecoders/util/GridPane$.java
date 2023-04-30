package com.wisecoders.util;


import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;


public class GridPane$ extends GridPane {

    public static final Insets insets = new Insets(10,10,10,10);
    public static final Insets insetsLowTop = new Insets(5,10,15,15);
    public static final Insets insetsSmall = new Insets(3,3,3,3);
    public GridPane$(){}

    public GridPane$ withGap(){
        this.setHgap( 12);
        this.setVgap( 10);
        return this;
    }

    public GridPane$ withLargeGap(){
        this.setHgap( 15);
        this.setVgap( 20);
        return this;
    }

    public GridPane$ withGap( double gap ) {
        return withGap( gap, gap );
    }
    public GridPane$ withGap( double h, double v ){
        this.setHgap( h);
        this.setVgap( v);
        return this;
    }

    public GridPane$ withPadding(){
        this.setPadding( insets );
        return this;
    }

    public GridPane$ withPaddingLowTop(){
        this.setPadding( insetsLowTop );
        return this;
    }

    public GridPane$ withSmallGapAndPadding() {
        setPadding( insetsSmall );
        return withSmallGap();
    }
    public GridPane$ withSmallGap() {
        setHgap( 8 );
        setHgap( 5 );
        return this;
    }

    public GridPane$ withSmallPadding(){
        this.setPadding( insetsSmall );
        return this;
    }

    public GridPane$ withPadding( double padding){
        this.setPadding( new Insets( padding) );
        return this;
    }

    public GridPane$ withGapAndPadding(){
        return withGap().withPadding();
    }

    public GridPane$ add(Node node, String position ){
        String[] pos = position.replaceAll(" ", "").split(",");
        int col = Integer.parseInt( pos[0]);
        int row = Integer.parseInt( pos[1]);
        int colSpan = 1, rowSpan = 1;
        if ( pos.length > 2 ){
            switch ( pos[2]){
                case "f" :
                    GridPane.setFillWidth( node, true);
                    GridPane.setHgrow( node, Priority.ALWAYS );
                    break;
                case "l" : GridPane.setHalignment( node, HPos.LEFT); break;
                case "c" : GridPane.setHalignment( node, HPos.CENTER); break;
                case "r" : GridPane.setHalignment( node, HPos.RIGHT); break;
                default: colSpan = Integer.parseInt( pos[2]) - col + 1; break;
            }
            switch ( pos[3]){
                case "f" :
                    GridPane.setFillHeight( node, true);
                    //GridPane.setVgrow( node, Priority.ALWAYS);
                    break;
                case "t" : GridPane.setValignment( node, VPos.TOP); break;
                case "c" : GridPane.setValignment( node, VPos.CENTER); break;
                case "b" : GridPane.setValignment( node, VPos.BOTTOM); break;
                default: rowSpan = Integer.parseInt( pos[3]) - row + 1; break;
            }
        }
        if ( pos.length > 4 ){
            switch ( pos[4]){
                case "f" :
                    GridPane.setFillWidth( node, true);
                    GridPane.setHgrow( node, Priority.ALWAYS );
                    break;
                case "l" : GridPane.setHalignment( node, HPos.LEFT); break;
                case "c" : GridPane.setHalignment( node, HPos.CENTER); break;
                case "r" : GridPane.setHalignment( node, HPos.RIGHT); break;
            }
            switch ( pos[5]){
                case "f" :
                    GridPane.setFillHeight( node, true);
                    //GridPane.setVgrow( node, Priority.ALWAYS);
                    break;
                case "t" : GridPane.setValignment( node, VPos.TOP); break;
                case "c" : GridPane.setValignment( node, VPos.CENTER); break;
                case "b" : GridPane.setValignment( node, VPos.BOTTOM); break;
            }
        }
        add(node, col, row, colSpan, rowSpan);
        return this;
    }

    public GridPane$ withColumns(int... priorities ){
        int index = 0;
        for ( int priority : priorities){
            if ( index < getColumnConstraints().size() ){
                ColumnConstraints cns = getColumnConstraints().get( index);
                switch (priority) {
                    case -1:
                        setColumn(cns, Priority.ALWAYS);
                        break;
                    case -2:
                        setColumn(cns, Priority.NEVER);
                        break;
                    default :
                        if ( priority > 0 ) {
                            cns.setHgrow(Priority.NEVER);
                            cns.setFillWidth(false);
                            cns.setPrefWidth(priority);
                        }
                        break;
                }
            } else {
                switch (priority) {
                    case -1:
                        addColumn(Priority.ALWAYS);
                        break;
                    case -2:
                        addColumn(Priority.NEVER);
                        break;
                    default :
                        if ( priority > 0 ) {
                            ColumnConstraints cns = new ColumnConstraints();
                            cns.setHgrow(Priority.NEVER);
                            cns.setFillWidth(false);
                            cns.setPrefWidth(priority);
                            getColumnConstraints().add( cns );
                        }
                        break;
                }
            }
            index++;
        }
        return this;
    }

    public GridPane$ withRows(int... priorities ){
        for ( int priority : priorities){
            switch ( priority ){
                case -1 : addRow(Priority.ALWAYS); break;
                case -2 : addRow(Priority.NEVER); break;
                default : if ( priority > 0 ) {
                    RowConstraints cns = new RowConstraints();
                    cns.setVgrow(Priority.NEVER);
                    cns.setFillHeight(false);
                    cns.setPrefHeight( priority );
                    getRowConstraints().add(cns);
                    break;
                }
            }
        }
        return this;
    }

    public GridPane$ addColumn(Priority priority ){
        ColumnConstraints cns = new ColumnConstraints();
        setColumn( cns, priority );
        getColumnConstraints().add( cns );
        return this;
    }

    private void setColumn( ColumnConstraints cns, Priority priority ){
        cns.setHgrow( priority );
        cns.setFillWidth( priority == Priority.ALWAYS );
    }

    public GridPane$ addRow(Priority priority ){
        RowConstraints cns = new RowConstraints();
        cns.setVgrow( priority );
        cns.setFillHeight( priority == Priority.ALWAYS );
        getRowConstraints().add( cns );
        return this;
    }

    public Node getNodeAt( int col, int row ){
        for ( Node node : getChildren() ) {
            if (getRowIndex(node) == row && getColumnIndex(node) == col) return node;
        }
        return null;
    }

    public GridPane$ withHorizontalChildren( Node... nodes ){
        int col = 0;
        for( Node node : nodes ){
            add( node, col++, 0, 1,1 );
        }
        return this;
    }
}
