/*
 * DensiTree.java Copyright (C) 2022 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package splitstree6.densitree;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import jloda.fx.control.RichTextLabel;
import jloda.fx.util.GeometryUtilsFX;
import jloda.graph.NodeArray;
import jloda.phylo.PhyloTree;
import jloda.util.RandomGaussian;
import splitstree6.view.splits.layout.RadialLabelLayout;

import java.io.IOException;
import java.util.Arrays;

/**
 * draw the densi-tree
 */
public class DensiTree {

    public static void clear(Canvas canvas, Pane pane) {
        var gc = canvas.getGraphicsContext2D();
        gc.restore();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        pane.getChildren().clear();
    }

    public static void draw(Parameters parameters, Model model, Canvas canvas, Pane pane) {
        System.out.println("Canvas:");
        System.err.println("Width: " + canvas.getWidth());
        System.err.println("Height: " + canvas.getHeight());

        var gc = canvas.getGraphicsContext2D();
        gc.setFont(Font.font("Courier New", 11));
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setStroke(Color.BLACK);

        pane.getChildren().clear();

        System.out.println("Pane:");
        System.out.println("Width: " + pane.getWidth());
        System.out.println("Height: " + pane.getHeight());


        int xmin = 100;
        int ymin = 100;
        int xmax = (int) (canvas.getWidth() - 100);
        int ymax = (int) (canvas.getHeight() - 100);

        boolean toScale = parameters.toScale;
        boolean jitter = parameters.jitter;
        boolean consensus = parameters.consensus;
        String highlight = parameters.highlight + ",";
        String labelMethod = parameters.labelMethod;

        int nTrees = model.getTreesBlock().size();
        int nTaxa = model.getTaxaBlock().getNtax();

        var labelLayout = new RadialLabelLayout();
        labelLayout.setGap(1);

        int[] circle = model.getCircularOrdering();
        String[] labels = new String[nTaxa];
        double[][] coords = new double[nTaxa][2];
        double[][][] coords2 = new double[nTaxa][nTrees][2];

        RandomGaussian random = new RandomGaussian(0, 4, 187);
        double shift = 0;

        var tree1 = model.getTreesBlock().getTree(1);
        int counter = 0;
        for (int j : circle) {
            if (j > 0) {
                labels[counter] = tree1.getTaxon2Node(j).getLabel();
                counter++;
            }
        }

        //Pair[] angles = getAngles(model);

        gc.setLineWidth(1.0);
        //drawLabels(model, rmax, x0, y0, gc);

        gc.save();
        //gc.translate(x0, y0);
        gc.setLineWidth(0.01);
        for (int i = 1; i <= nTrees; i++) {
            var tree = model.getTreesBlock().getTree(i);
            shift = random.nextDouble();

            if (labelMethod.contains("median")) {
                drawEdges1(tree, i - 1, circle, gc, xmin, ymin, xmax, ymax, toScale, jitter, shift, coords2, labels);
            } else {
                drawEdges(tree, circle, gc, xmin, ymin, xmax, ymax, toScale, jitter, shift, coords, labels, labelLayout);
            }
        }

        if (labelMethod.contains("mean")) {
            meanLabels(tree1, circle, pane, xmin, ymin, xmax, ymax, toScale, coords, labels, nTrees);
        } else if (labelMethod.contains("median")) {
            medianLabels(tree1, circle, pane, xmin, ymin, xmax, ymax, toScale, coords2, labels);
        } else if (labelMethod.contains("radial")){
            radialLabels(tree1, circle, pane, xmin, ymin, xmax, ymax, toScale, coords, labels, nTrees, labelLayout);
            labelLayout.layoutLabels();
        }

        gc.setLineWidth(0.5);


        //System.out.println(highlight);
        if (highlight.matches("[\\d\\,]*")) {
            String[] specTrees = parameters.highlight.split("\\,");
            gc.setStroke(Color.GREENYELLOW);
            for (String specTree : specTrees) {
                int treeNum = Integer.parseInt(specTree);
                if (treeNum > 0 && treeNum <= nTrees) {
                    var tree = model.getTreesBlock().getTree(Integer.parseInt(specTree));
                    drawEdges(tree, circle, gc, xmin, ymin, xmax, ymax, toScale, false, 0, coords, labels, labelLayout);
                }
            }
        }

        if (consensus) {
            try {
                var consensusTree = MajorityConsensus.apply(model);
                gc.setStroke(Color.BLUE);
                drawEdges(consensusTree, circle, gc, xmin, ymin, xmax, ymax, toScale, false, 0, coords, labels, labelLayout);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        gc.setStroke(Color.BLACK);
    }


    public static void meanLabels(
            PhyloTree tree, int[] circle, Pane pane, int xmin, int ymin, int xmax, int ymax, boolean toScale,
            double[][] coords, String[] labels, int nTrees
    ) {
        NodeArray<Point2D> nodePointMap = tree.newNodeArray();
        var nodeAngleMap = tree.newNodeDoubleArray();
        LayoutAlgorithm.apply(tree, toScale, circle, nodePointMap, nodeAngleMap);
        adjustCoordinatesToBox(nodePointMap, xmin, ymin, xmax, ymax);

        for (var e : tree.edges()) {
            var v = e.getSource();
            var w = e.getTarget();
            var wPt = nodePointMap.get(w);
            if (e.getTarget().isLeaf()) {
                var label = new RichTextLabel(tree.getLabel(w));
                for (int i = 0; i < labels.length; i++) {
                    if (tree.getLabel(w).equals(labels[i])) {
                        int finalI = i;
                        ChangeListener<Number> listener = (observableValue, oldValue, newValue) -> { // use a listener because we have to wait until both width and height have been set
                            if (oldValue.doubleValue() == 0 && newValue.doubleValue() > 0 && label.getWidth() > 0 && label.getHeight() > 0) {
                                var angle = nodeAngleMap.get(w);
                                var delta = GeometryUtilsFX.translateByAngle(-0.5 * label.getWidth(), -0.5 * label.getHeight(), angle, 0.5 * label.getWidth() + 5);
                                label.setLayoutX(coords[finalI][0] / nTrees + delta.getX());
                                label.setLayoutY(coords[finalI][1] / nTrees + delta.getY());
                                label.setRotate(angle);
                                label.ensureUpright();
                                label.setTextFill(Color.RED);
                            }
                        };
                        label.widthProperty().addListener(listener);
                        label.heightProperty().addListener(listener);
                        pane.getChildren().add(label);
                        break;
                    }
                }
            }
        }
    }

    public static void medianLabels(
            PhyloTree tree, int[] circle, Pane pane, int xmin, int ymin, int xmax, int ymax, boolean toScale, double[][][] coords2, String[] labels
    ) {
        NodeArray<Point2D> nodePointMap = tree.newNodeArray();
        var nodeAngleMap = tree.newNodeDoubleArray();
        LayoutAlgorithm.apply(tree, toScale, circle, nodePointMap, nodeAngleMap);
        adjustCoordinatesToBox(nodePointMap, xmin, ymin, xmax, ymax);

        for (var e : tree.edges()) {
            var v = e.getSource();
            var w = e.getTarget();
            var wPt = nodePointMap.get(w);
            if (e.getTarget().isLeaf()) {
                var label = new RichTextLabel(tree.getLabel(w));
                for (int i = 0; i < labels.length; i++) {
                    if (tree.getLabel(w).equals(labels[i])) {
                        int finalI = i;
                        Arrays.sort(coords2[finalI], (a, b) -> Double.compare(a[0], b[0]));
                        ChangeListener<Number> listener = (observableValue, oldValue, newValue) -> { // use a listener because we have to wait until both width and height have been set
                            if (oldValue.doubleValue() == 0 && newValue.doubleValue() > 0 && label.getWidth() > 0 && label.getHeight() > 0) {
                                var angle = nodeAngleMap.get(w);
                                var delta = GeometryUtilsFX.translateByAngle(-0.5 * label.getWidth(), -0.5 * label.getHeight(), angle, 0.5 * label.getWidth() + 5);
                                label.setLayoutX(coords2[finalI][coords2[finalI].length / 2][0] + delta.getX());
                                label.setLayoutY(coords2[finalI][coords2[finalI].length / 2][1] + delta.getY());
                                label.setRotate(angle);
                                label.ensureUpright();
                                label.setTextFill(Color.RED);
                            }
                        };
                        label.widthProperty().addListener(listener);
                        label.heightProperty().addListener(listener);
                        pane.getChildren().add(label);
                        break;
                    }
                }
            }
        }
    }

        public static void radialLabels(
                PhyloTree tree, int[] circle, Pane pane, int xmin, int ymin, int xmax, int ymax, boolean toScale,
        double[][] coords, String[] labels, int nTrees, RadialLabelLayout labelLayout
    ) {
            NodeArray<Point2D> nodePointMap = tree.newNodeArray();
            var nodeAngleMap = tree.newNodeDoubleArray();
            LayoutAlgorithm.apply(tree, toScale, circle, nodePointMap, nodeAngleMap);
            adjustCoordinatesToBox(nodePointMap, xmin, ymin, xmax, ymax);

            for (var e : tree.edges()) {
                var v = e.getSource();
                var w = e.getTarget();
                var wPt = nodePointMap.get(w);
                if (e.getTarget().isLeaf()) {
                    var label = new RichTextLabel(tree.getLabel(w));
                    for (int i = 0; i < labels.length; i++) {
                        if (tree.getLabel(w).equals(labels[i])) {
                            int finalI = i;
                            ChangeListener<Number> listener = (observableValue, oldValue, newValue) -> { // use a listener because we have to wait until both width and height have been set
                                if (oldValue.doubleValue() == 0 && newValue.doubleValue() > 0 && label.getWidth() > 0 && label.getHeight() > 0) {
                                    var angle = nodeAngleMap.get(w);
                                    var delta = GeometryUtilsFX.translateByAngle(-0.5 * label.getWidth(), -0.5 * label.getHeight(), angle, 0.5 * label.getWidth() + 5);
                                    label.setLayoutX(coords[finalI][0] / nTrees + delta.getX());
                                    label.setLayoutY(coords[finalI][1] / nTrees + delta.getY());
                                    //label.setRotate(angle);
                                    label.ensureUpright();
                                    label.setTextFill(Color.RED);
                                }
                            };
                            label.widthProperty().addListener(listener);
                            label.heightProperty().addListener(listener);
                            pane.getChildren().add(label);
                            labelLayout.addItem(label.translateXProperty(),label.translateYProperty(),nodeAngleMap.get(w),label.widthProperty(),label.heightProperty(),
                                    xOffset -> {
                                        label.setTranslateX(xOffset);
                                    },
                                    yOffset -> {
                                        label.setTranslateY(yOffset);
                                        //label.setVisible(true);
                                    });
                            break;
                        }
                    }
                }
            }
        }

    public static void drawEdges(
            PhyloTree tree, int[] circle, GraphicsContext gc, int xmin, int ymin, int xmax, int ymax, boolean toScale,
            boolean jitter, double shift, double[][] coords, String[] labels, RadialLabelLayout labelLayout
    ) {
        NodeArray<Point2D> nodePointMap = tree.newNodeArray();
        var nodeAngleMap = tree.newNodeDoubleArray();
        LayoutAlgorithm.apply(tree, toScale, circle, nodePointMap, nodeAngleMap);
        adjustCoordinatesToBox(nodePointMap, xmin, ymin, xmax, ymax);

        for (var e : tree.edges()) {
            var v = e.getSource();
            var w = e.getTarget();
            var vPt = nodePointMap.get(v);
            var wPt = nodePointMap.get(w);

            if (w.isLeaf()) {
                for (int i = 0; i < labels.length; i++) {
                    if (tree.getLabel(w).equals(labels[i])) {
                        coords[i][0] += wPt.getX();
                        coords[i][1] += wPt.getY();
                        break;
                    }
                }
                SimpleDoubleProperty x = new SimpleDoubleProperty(wPt.getX());
                SimpleDoubleProperty y = new SimpleDoubleProperty(wPt.getY());
                // labelLayout.addAvoidable(x,y,1,1);
            }

            if (jitter) {
                gc.strokeLine(vPt.getX() + shift, vPt.getY(), wPt.getX() + shift, wPt.getY());
            } else {
                gc.strokeLine(vPt.getX(), vPt.getY(), wPt.getX(), wPt.getY());
            }
        }
    }

    public static void drawEdges1(
            PhyloTree tree, int treeNum, int[] circle, GraphicsContext gc, int xmin, int ymin, int xmax, int ymax,
            boolean toScale, boolean jitter, double shift, double[][][] coords2, String[] labels
    ) {
        NodeArray<Point2D> nodePointMap = tree.newNodeArray();
        var nodeAngleMap = tree.newNodeDoubleArray();
        LayoutAlgorithm.apply(tree, toScale, circle, nodePointMap, nodeAngleMap);
        adjustCoordinatesToBox(nodePointMap, xmin, ymin, xmax, ymax);

        for (var e : tree.edges()) {
            var v = e.getSource();
            var w = e.getTarget();
            var vPt = nodePointMap.get(v);
            var wPt = nodePointMap.get(w);

            if (w.isLeaf()) {
                for (int i = 0; i < labels.length; i++) {
                    if (tree.getLabel(w).equals(labels[i])) {
                        coords2[i][treeNum][0] += wPt.getX();
                        coords2[i][treeNum][1] += wPt.getY();
                        break;
                    }
                }
            }

            if (jitter) {
                gc.strokeLine(vPt.getX() + shift, vPt.getY(), wPt.getX() + shift, wPt.getY());
            } else {
                gc.strokeLine(vPt.getX(), vPt.getY(), wPt.getX(), wPt.getY());
            }
        }
    }


    public static void adjustCoordinatesToBox(NodeArray<Point2D> nodePointMap, double xMinTarget, int yMinTarget, double xMaxTarget, double yMaxTarget) {

        var xMin = nodePointMap.values().stream().mapToDouble(Point2D::getX).min().orElse(0);
        var xMax = nodePointMap.values().stream().mapToDouble(Point2D::getX).max().orElse(0);
        var yMin = nodePointMap.values().stream().mapToDouble(Point2D::getY).min().orElse(0);
        var yMax = nodePointMap.values().stream().mapToDouble(Point2D::getY).max().orElse(0);

        var scaleX = (xMaxTarget - xMinTarget) / (xMax - xMin);
        var scaleY = (yMaxTarget - yMinTarget) / (yMax - yMin);
        var scale = Math.min(scaleX, scaleY);

        for (var v : nodePointMap.keySet()) {
            var point = nodePointMap.get(v);
            nodePointMap.put(v, point.multiply(scale).add(0.5 * (xMinTarget + xMaxTarget), 0.5 * (yMinTarget + yMaxTarget)));
        }
    }

    /**
     * this contains all the parameters used for drawing
     */
    public record Parameters(boolean toScale,
                             boolean jitter,
                             boolean consensus,
                             String highlight,
                             String labelMethod) {
    }
}