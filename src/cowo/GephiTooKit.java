///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//package cowo;
//
//import java.awt.Color;
//import java.io.File;
//import java.io.IOException;
//import org.gephi.data.attributes.api.AttributeColumn;
//import org.gephi.data.attributes.api.AttributeController;
//import org.gephi.data.attributes.api.AttributeModel;
//import org.gephi.filters.api.FilterController;
//import org.gephi.filters.api.Query;
//import org.gephi.filters.api.Range;
//import org.gephi.filters.plugin.edge.EdgeWeightBuilder.EdgeWeightFilter;
//import org.gephi.filters.plugin.graph.DegreeRangeBuilder.DegreeRangeFilter;
//import org.gephi.graph.api.*;
//import org.gephi.io.exporter.api.ExportController;
//import org.gephi.io.importer.api.Container;
//import org.gephi.io.importer.api.EdgeDefault;
//import org.gephi.io.importer.api.ImportController;
//import org.gephi.io.processor.plugin.DefaultProcessor;
//import org.gephi.layout.plugin.force.StepDisplacement;
//import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
//import org.gephi.preview.api.PreviewController;
//import org.gephi.preview.api.PreviewModel;
//import org.gephi.preview.api.PreviewProperty;
//import org.gephi.preview.types.EdgeColor;
//import org.gephi.project.api.ProjectController;
//import org.gephi.project.api.Workspace;
//import org.gephi.ranking.api.Interpolator;
//import org.gephi.ranking.api.Ranking;
//import org.gephi.ranking.api.RankingController;
//import org.gephi.ranking.api.Transformer;
//import org.gephi.ranking.plugin.transformer.AbstractColorTransformer;
//import org.gephi.ranking.plugin.transformer.AbstractSizeTransformer;
//import org.gephi.statistics.plugin.GraphDistance;
//import org.openide.util.Lookup;
//
///**
// *
// * @author C. Levallois
// */
//public class GephiTooKit {
//
//    /**
//     * @param args the command line arguments
//     */
//    static void main(String wkOutput,String fileGMLName) {
////Init a project - and therefore a workspace
//ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
//pc.newProject();
//Workspace workspace = pc.getCurrentWorkspace();
// 
////Get models and controllers for this new workspace - will be useful later
//AttributeModel attributeModel = Lookup.getDefault().lookup(AttributeController.class).getModel();
//GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
//PreviewModel model = Lookup.getDefault().lookup(PreviewController.class).getModel();
//ImportController importController = Lookup.getDefault().lookup(ImportController.class);
//FilterController filterController = Lookup.getDefault().lookup(FilterController.class);
//RankingController rankingController = Lookup.getDefault().lookup(RankingController.class);
// 
////Import file       
//Container container;
//try {
//    File file = new File(wkOutput + fileGMLName);
//    container = importController.importFile(file);
//    container.getLoader().setEdgeDefault(EdgeDefault.DIRECTED);   //Force DIRECTED
//} catch (Exception ex) {
//    ex.printStackTrace();
//    return;
//}
// 
////Append imported data to GraphAPI
//importController.process(container, new DefaultProcessor(), workspace);
// 
////See if graph is well imported
//DirectedGraph graph = graphModel.getDirectedGraph();
//System.out.println("Nodes: " + graph.getNodeCount());
//System.out.println("Edges: " + graph.getEdgeCount());
// 
////Filter      
//DegreeRangeFilter degreeFilter = new DegreeRangeFilter();
//EdgeWeightFilter edgeWeightFilter = new EdgeWeightFilter();
//
////degreeFilter.init(graph);
//edgeWeightFilter.init(graph);
////degreeFilter.setRange(new Range(500, Integer.MAX_VALUE));     //Remove nodes with degree < 500
////edgeWeightFilter.setRange(new Range(10, Integer.MAX_VALUE));     //Remove nodes with degree < 500
//Query query = filterController.createQuery(degreeFilter);
//GraphView view = filterController.filter(query);
//graphModel.setVisibleView(view);    //Set the filter result as the visible view
// 
////See visible graph stats
//UndirectedGraph graphVisible = graphModel.getUndirectedGraphVisible();
//System.out.println("Removed words connected to less than 500 other words");
//System.out.println("Nodes: " + graphVisible.getNodeCount());
//System.out.println("Edges: " + graphVisible.getEdgeCount());
// 
////Run YifanHuLayout for 100 passes - The layout always takes the current visible view
//YifanHuLayout layout = new YifanHuLayout(null, new StepDisplacement(1f));
//layout.setGraphModel(graphModel);
//layout.resetPropertiesValues();
//layout.setOptimalDistance(200f);
// 
//for (int i = 0; i < 100 && layout.canAlgo(); i++) {
//    layout.goAlgo();
//}
// 
////Get Centrality
//GraphDistance distance = new GraphDistance();
//distance.setDirected(true);
//distance.execute(graphModel, attributeModel);
// 
////Rank color by Degree
//Ranking degreeRanking = rankingController.getModel().getRanking(Ranking.NODE_ELEMENT, Ranking.DEGREE_RANKING);
//AbstractColorTransformer colorTransformer = (AbstractColorTransformer) rankingController.getModel().getTransformer(Ranking.NODE_ELEMENT, Transformer.RENDERABLE_COLOR);
//colorTransformer.setColors(new Color[]{new Color(0xFEF0D9), new Color(0xB30000)});
//rankingController.transform(degreeRanking,colorTransformer);
//
// 
////Rank LABEL size by degree 
////AttributeColumn centralityColumn = attributeModel.getNodeTable().getColumn(GraphDistance.BETWEENNESS);
////Ranking centralityRanking = rankingController.getModel().getRanking(Ranking.NODE_ELEMENT, centralityColumn.getId());
//AbstractSizeTransformer sizeTransformer = (AbstractSizeTransformer) rankingController.getModel().getTransformer(Ranking.NODE_ELEMENT, Transformer.LABEL_SIZE);
//sizeTransformer.setMinSize(3);
//sizeTransformer.setMaxSize(10);
//rankingController.setInterpolator(new Interpolator.BezierInterpolator(new Float(0), new Float(1), new Float(0), new Float(1)));
//rankingController.transform(degreeRanking,sizeTransformer);
// 
////Preview
//model.getProperties().putValue(PreviewProperty.SHOW_NODE_LABELS, Boolean.TRUE);
//model.getProperties().putValue(PreviewProperty.EDGE_COLOR, new EdgeColor(Color.GRAY));
//model.getProperties().putValue(PreviewProperty.SHOW_EDGES,false);
//model.getProperties().putValue(PreviewProperty.EDGE_THICKNESS, new Float(0.1f));
//model.getProperties().putValue(PreviewProperty.NODE_OPACITY, new Float(0.00));
////model.getProperties().putValue(PreviewProperty.NODE_LABEL_FONT, model.getProperties().getFontValue(PreviewProperty.NODE_LABEL_FONT).deriveFont(8));
// 
////Export
//ExportController ec = Lookup.getDefault().lookup(ExportController.class);
//try {
//    ec.exportFile(new File(wkOutput.concat("GEPHI_output.png")));
//} catch (IOException ex) {
//    ex.printStackTrace();
//    return;
//}
//    }
//}
