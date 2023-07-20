/*
 *  TreeSheet.java Copyright (C) 2023 Daniel H. Huson
 *
 *  (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package splitstree6.xtra.genetreeview.layout;

import javafx.beans.property.*;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import jloda.fx.control.RichTextLabel;
import jloda.fx.util.BasicFX;
import jloda.fx.util.SelectionEffectBlue;
import jloda.phylo.PhyloTree;
import splitstree6.layout.tree.ComputeTreeLayout;
import splitstree6.layout.tree.HeightAndAngles;
import splitstree6.layout.tree.LabeledEdgeShape;
import splitstree6.layout.tree.TreeDiagramType;
import splitstree6.xtra.genetreeview.SelectionModel;
import splitstree6.xtra.genetreeview.SelectionModelSet;

import java.util.HashMap;
import java.util.function.Function;

public class TreeSheet extends StackPane implements Selectable {

    private final int id;
    private final double width;
    private final double height;
    private Rectangle selectionRectangle;
    private Text nameLabel;
    private final Group taxonLabels;
    private final Group edges;
    private final BooleanProperty isSelectedProperty = new SimpleBooleanProperty();
    private final LongProperty lastUpdate = new SimpleLongProperty(this, "lastUpdate", 0L);

    public TreeSheet(PhyloTree tree, int id, double width, double height, TreeDiagramType diagram,
                     SelectionModelSet<Integer> taxaSelectionModel, SelectionModelSet<Integer> edgeSelectionModel) {
        this.id = id;
        this.width = width;
        this.height = height;

        createTreeBackground(tree.getName());

        Function<Integer, StringProperty> taxonLabelMap = (taxonId) ->
                new SimpleStringProperty(tree.getTaxon2Node(taxonId).getLabel());

        // Computing the actual tree with splitstree6.layout.tree.ComputeTreeLayout
        /*Group layoutedTree = ComputeTreeLayout.apply(tree, tree.getNumberOfTaxa(), taxonLabelMap,
                diagram, HeightAndAngles.Averaging.ChildAverage,width-80,height-20,
                false, new HashMap<>(), new HashMap<>()).getAllAsGroup();*/

        var computedTreeLayout = ComputeTreeLayout.apply(tree, tree.getNumberOfTaxa(), taxonLabelMap,
                diagram, HeightAndAngles.Averaging.ChildAverage,width-80,height-20,
                false, new HashMap<>(), new HashMap<>());

        Group layoutedTree = new Group();
        if (computedTreeLayout.labelConnectors() != null)
            layoutedTree.getChildren().add(computedTreeLayout.labelConnectors());
        if (computedTreeLayout.edges() != null)
            layoutedTree.getChildren().add(computedTreeLayout.edges());
        if (computedTreeLayout.nodes() != null)
            layoutedTree.getChildren().add(computedTreeLayout.nodes());
        if (computedTreeLayout.otherLabels() != null)
            layoutedTree.getChildren().add(computedTreeLayout.otherLabels());
        if (computedTreeLayout.taxonLabels() != null)
            layoutedTree.getChildren().add(computedTreeLayout.taxonLabels());
        this.edges = computedTreeLayout.edges();
        this.taxonLabels = computedTreeLayout.taxonLabels();

        // Adjusting label font size
        if (diagram.isRadialOrCircular()) {
            assert taxonLabels != null;
            for (var label : taxonLabels.getChildren()) {
                ((RichTextLabel)label).setScale(0.3);
            }
        } else if (diagram.equals(TreeDiagramType.RectangularCladogram) |
                diagram.equals(TreeDiagramType.RectangularPhylogram)) {
            assert taxonLabels != null;
            for (var label : taxonLabels.getChildren()) {
                ((RichTextLabel)label).setScale(0.4);
            }
            layoutedTree.setTranslateY(5); // space on top needed for name label
        }
        else { // for triangular
            assert taxonLabels != null;
            for (var label : taxonLabels.getChildren()) {
                ((RichTextLabel)label).setScale(0.4);
            }
        }
        this.getChildren().addAll(layoutedTree);

        // Taxon Selection
        HashMap<String, Integer> taxonName2id = new HashMap<>();
        for (var taxonId : tree.getTaxonNodeMap().keySet())
            taxonName2id.put(tree.getTaxon2Node(taxonId).getLabel(),taxonId);
        for (var taxonLabel : taxonLabels.getChildren()) {
            RichTextLabel taxonRichTextLabel = (RichTextLabel) taxonLabel;
            taxonLabel.setOnMouseEntered(e -> taxonRichTextLabel.setScale(1.1*taxonRichTextLabel.getScale()));
            taxonLabel.setOnMouseExited(e -> taxonRichTextLabel.setScale(1/1.1*taxonRichTextLabel.getScale()));
            int taxonId = taxonName2id.get(taxonRichTextLabel.getText());
            if (taxaSelectionModel.getSelectedItems().contains(taxonId)) selectTaxon(taxonRichTextLabel.getText(),true);
            taxonLabel.setOnMouseClicked(e -> {
                boolean selectedBefore = taxaSelectionModel.getSelectedItems().contains(taxonId);
                if (!e.isShiftDown()) {
                    taxaSelectionModel.clearSelection();
                    edgeSelectionModel.clearSelection();
                    if (!selectedBefore) {
                        taxaSelectionModel.select(taxonId);
                        updateEdgeSelection();
                    }
                } else {
                    taxaSelectionModel.setSelected(taxonId, !selectedBefore);
                    updateEdgeSelection();
                }
            });
        }

        // Edge Selection
        for (var edge : edges.getChildren()) {
            var labeledEdgeShape = (LabeledEdgeShape) edge;
            for (var node : labeledEdgeShape.all()) {
                if (node instanceof Shape shape) {
                    shape.setOnMouseEntered(e -> shape.setStrokeWidth(shape.getStrokeWidth() + 3));
                    shape.setOnMouseExited(e -> shape.setStrokeWidth(shape.getStrokeWidth() - 3));
                    //shape.setOnMouseClicked(e -> selectEdge(shape,true));
                }
                else if (node instanceof RichTextLabel label) {
                    label.setOnMouseEntered(e -> label.setScale(1.1*label.getScale()));
                    label.setOnMouseExited(e -> label.setScale(1/1.1*label.getScale()));
                }
            }
            int edgeIndex = edges.getChildren().indexOf(edge);
            if (edgeSelectionModel.getSelectedItems().contains(edgeIndex)) selectEdge(edge,true);
            edge.setOnMouseClicked(e -> {
                boolean selectedBefore = edgeSelectionModel.getSelectedItems().contains(edgeIndex);
                if (!e.isShiftDown()) {
                    taxaSelectionModel.clearSelection();
                    edgeSelectionModel.clearSelection();
                    if (!selectedBefore) {
                        edgeSelectionModel.select(edgeIndex);
                        updateEdgeSelection();
                        updateTaxaSelection();
                    }
                } else {
                    edgeSelectionModel.setSelected(edgeIndex, !selectedBefore);
                    updateEdgeSelection();
                    updateTaxaSelection();
                }
                lastUpdate.set(System.currentTimeMillis());
            });
        }

        // Tree Selection
        isSelectedProperty.addListener((observableValue, wasSelected, isSelected) -> {
            if (isSelected) {
                this.setStyle("-fx-border-color: -fx-accent");
            }
            else {
                this.setStyle("-fx-border-color: -fx-box-border");
            }
            lastUpdate.set(System.currentTimeMillis());
        });
    }

    private void createTreeBackground(String treeName) {
        this.setPrefSize(width,height);
        this.setBackground(new Background(new BackgroundFill(Color.web("white",1),null,null)));
        this.setStyle("-fx-border-color: -fx-box-border; -fx-display:inline;");
        var nameLabelContainer = new Pane();
        nameLabel = new Text(treeName);
        nameLabel.setFont(new Font(9));
        nameLabel.setLayoutX(2);
        nameLabel.setLayoutY(9);
        nameLabelContainer.getChildren().add(nameLabel);
        this.getChildren().add(nameLabelContainer);

        selectionRectangle = new Rectangle(width, height, Color.TRANSPARENT);
        this.getChildren().add(selectionRectangle);
    }

    public boolean selectTaxon(String taxonName, boolean select) {
        for (var label : taxonLabels.getChildren()) {
            if (((RichTextLabel)label).getText().equals(taxonName)) {
                // Selection indication like in splitstree
                if (select) label.setEffect(SelectionEffectBlue.getInstance());
                else label.setEffect(null);
                // Alternative: Selection indication with CSS style ( no suitable color yet)
                //if (select) label.setStyle("-fx-effect: dropshadow(one-pass-box,-fx-focus-color,5,1,0,0)");
                //else label.setStyle("-fx-effect: dropshadow(one-pass-box,transparent,5,1,0,0)");
                lastUpdate.set(System.currentTimeMillis());
                return true;
            }
        }
        return false;
    }

    public boolean selectEdge(int edgeId, boolean select) {
        var edge = edges.getChildren().get(edgeId);
        if (edge != null) {
            selectEdge(edge,select);
            return true;
        }
        return false;
    }

    private void selectEdge(Node edge, boolean select) {
        if (select) edge.setEffect(SelectionEffectBlue.getInstance());
        else edge.setEffect(null);
        lastUpdate.set(System.currentTimeMillis());
    }

    public void updateEdgeSelection() {
        // TODO
    }

    private void updateTaxaSelection() {
        // TODO
    }

    public int getTreeId() {
        return id;
    }

    public Rectangle getSelectionRectangle() {
        return selectionRectangle;
    };

    public void setTreeName(String treeName) {
        nameLabel.setText(treeName);
        lastUpdate.set(System.currentTimeMillis());
    }

    public void setSelectedProperty(boolean selected) {
        isSelectedProperty.set(selected);
    };

    public void setSelectedProperty() {
        setSelectedProperty(!isSelectedProperty.get());
    }

    public BooleanProperty isSelectedProperty() {
        return isSelectedProperty;
    }

    public ReadOnlyLongProperty lastUpdateProperty() {
        return lastUpdate;
    }
}
