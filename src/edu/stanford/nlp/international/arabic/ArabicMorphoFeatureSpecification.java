package edu.stanford.nlp.international.arabic;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.international.morph.MorphoFeatureSpecification;
import edu.stanford.nlp.international.morph.MorphoFeatures;

/**
 * Extracts morphosyntactic features from BAMA/SAMA analyses. Compatible with both the
 * long tags in the ATB and the output of MADA.
 * 
 * @author Spence Green
 *
 */
public class ArabicMorphoFeatureSpecification extends MorphoFeatureSpecification {

  private static final long serialVersionUID = 4448045447200922076L;

  public static final String[] defVals = {"DT"};
  public static final String[] caseVals = {"NOM","ACC","GEN"};
  public static final String[] genVals = {"M","F"};
  public static final String[] numVals = {"SG","DU","PL"};
  public static final String[] perVals = {"1","2","3"};
  public static final String[] possVals = {"POSS"};
  public static final String[] voiceVals = {"ACT","PASS"};
  public static final String[] moodVals = {"I","S","J"};
  public static final String[] tenseVals = {"PAST","PRES","IMP"};
  
  private static final Pattern pPronom = Pattern.compile("PRON_(.+)$");
  
  //Verbal patterns
  private static final Pattern pMood = Pattern.compile("_MOOD:(.+)$");
  private static final Pattern pTbPrefix = Pattern.compile("^(FUT|JUS)\\+");
  private static final Pattern pPvFeats = Pattern.compile(":(.+)$");
  
  @Override
  public List<String> getValues(MorphoFeatureType feat) {
    if(feat == MorphoFeatureType.DEF)
      return Arrays.asList(defVals);
    else if(feat == MorphoFeatureType.CASE) {
      throw new RuntimeException(this.getClass().getName() + ": Case is presently unsupported!");
//      return Arrays.asList(caseVals);
    } else if(feat == MorphoFeatureType.GEN)
      return Arrays.asList(genVals);
    else if(feat == MorphoFeatureType.NUM)
      return Arrays.asList(numVals);
    else if(feat == MorphoFeatureType.PER)
      return Arrays.asList(perVals);
    else if(feat == MorphoFeatureType.POSS)
      return Arrays.asList(possVals);
    else if(feat == MorphoFeatureType.VOICE)
      return Arrays.asList(voiceVals);
    else if(feat == MorphoFeatureType.MOOD)
      return Arrays.asList(moodVals);
    else if(feat == MorphoFeatureType.TENSE)
      return Arrays.asList(tenseVals);
    else
      throw new IllegalArgumentException("Arabic does not support feature type: " + feat.toString());
  }
  /**
   * Parses a SAMA analysis into a MorphoFeatures spec.
   * 
   * TODO wsg: Presently, feature extraction only works for gold analyses
   */
  @Override
  public MorphoFeatures strToFeatures(String spec) {
    MorphoFeatures feats = new ArabicMorphoFeatures();
    
    //Usually this is the boundary symbol
    if(spec == null || spec.equals(""))
      return feats;
    
    //Definiteness
    if(spec.startsWith("DET") && isActive(MorphoFeatureType.DEF))
      feats.addFeature(MorphoFeatureType.DEF, defVals[0]);
    
    //Possessiveness
    if(spec.contains("POSS") && isActive(MorphoFeatureType.POSS))
      feats.addFeature(MorphoFeatureType.POSS,possVals[0]);
    
    //phi features -- nominals and pronominals
    //Mona ignores Pronominals in ERTS, but they seem to help...
    if(spec.contains("NSUFF") || spec.contains("REL_PRON") || spec.contains("INTERROG_PRON")) {
      if(isActive(MorphoFeatureType.GEN)) {
        if(spec.contains("MASC"))
          feats.addFeature(MorphoFeatureType.GEN, genVals[0]);
        else if(spec.contains("FEM"))
          feats.addFeature(MorphoFeatureType.GEN, genVals[1]);
      }

      if(isActive(MorphoFeatureType.NUM)) {
        if(spec.contains("SG"))
          feats.addFeature(MorphoFeatureType.NUM, numVals[0]);        
        else if(spec.contains("DU"))
          feats.addFeature(MorphoFeatureType.NUM, numVals[1]);
        else if(spec.contains("PL"))
          feats.addFeature(MorphoFeatureType.NUM, numVals[2]);
      }
    
    } else if(spec.contains("PRON")) {
      if(spec.startsWith("DEM_PRON"))
        feats.addFeature(MorphoFeatureType.DEF, defVals[0]);
      
      Matcher m = pPronom.matcher(spec);
      if(m.find()) {
        spec = m.group(1);
        processDefaultSpec(feats,spec);      
      }
    }
    
    //phi features -- verbs
    else if(spec.contains("+IV") || spec.contains("+CV") || spec.contains("+PV") || spec.equals("PV_PASS")) {
      
      //Strip off the jussive/future tense markers (which were the clitics ل and س respectively)
      spec = pTbPrefix.matcher(spec).replaceAll("");
      
      if(isActive(MorphoFeatureType.TENSE)) {
        if(spec.contains("PV"))
          feats.addFeature(MorphoFeatureType.TENSE, tenseVals[0]);
        else if(spec.contains("IV"))
          feats.addFeature(MorphoFeatureType.TENSE, tenseVals[1]);
        else if(spec.contains("CV"))
          feats.addFeature(MorphoFeatureType.TENSE, tenseVals[2]);
      }
      
      if(spec.startsWith("IV") && ! spec.equals("IV+IVSUFF_SUBJ:3FS")) {
        String[] toks = spec.split("\\+");
        String featStr = toks[0].replace("IV", "");
        processDefaultSpec(feats,featStr);
        
      } else {
        Matcher m = pPvFeats.matcher(spec);
        if(m.find()) {
          String featStr = m.group(1);
          processDefaultSpec(feats,featStr);
        } else if(spec.equals("PV+PVSUFF_3MS")) { //Special case in ATB3
          String[] toks = spec.split("_");
          processDefaultSpec(feats,toks[1]);
        }
      }
      
      if(isActive(MorphoFeatureType.MOOD)) {
        Matcher m = pMood.matcher(spec);
        if(m.find()) {
          String moodStr = m.group(1);
          if(moodStr.equals("I"))
            feats.addFeature(MorphoFeatureType.MOOD, moodVals[0]);
          else if(moodStr.equals("S"))
            feats.addFeature(MorphoFeatureType.MOOD, moodVals[1]);
          else if(moodStr.equals("J"))
            feats.addFeature(MorphoFeatureType.MOOD, moodVals[2]);
        }
      }
      
      if(isActive(MorphoFeatureType.VOICE)) {
        if(spec.contains("PASS"))
          feats.addFeature(MorphoFeatureType.VOICE, voiceVals[1]);
        else
          feats.addFeature(MorphoFeatureType.VOICE,voiceVals[0]);
      }
    }
        
    return feats;
  }
  
  private void processDefaultSpec(MorphoFeatures feats, String spec) {
    if(spec == null || spec.equals("")) return;
    
    if(isActive(MorphoFeatureType.GEN)) {
      if(spec.contains("M"))
        feats.addFeature(MorphoFeatureType.GEN, genVals[0]);
      else if(spec.contains("F"))
        feats.addFeature(MorphoFeatureType.GEN, genVals[1]);
    }
    
    if(isActive(MorphoFeatureType.NUM)) {
      if(spec.endsWith("S"))
        feats.addFeature(MorphoFeatureType.NUM, numVals[0]);        
      else if(spec.endsWith("D"))
        feats.addFeature(MorphoFeatureType.NUM, numVals[1]);
      else if(spec.endsWith("P"))
        feats.addFeature(MorphoFeatureType.NUM, numVals[2]);
    }
    
    if(isActive(MorphoFeatureType.PER)) {
      if(spec.contains("1"))
        feats.addFeature(MorphoFeatureType.PER, perVals[0]);
      else if(spec.contains("2"))
        feats.addFeature(MorphoFeatureType.PER, perVals[1]);
      else if(spec.contains("3"))
        feats.addFeature(MorphoFeatureType.PER, perVals[2]);
    }
  }
  
  /**
   * Creates tags that conform to the default "DT" prefix used elsewhere in the codebase
   * 
   * @author Spence Green
   *
   */
  public static class ArabicMorphoFeatures extends MorphoFeatures {
    
    private static final long serialVersionUID = -4611776415583633186L;

    @Override
    public MorphoFeatures fromTagString(String str) {
      String[] feats = str.split("\\-");
      MorphoFeatures mFeats = new ArabicMorphoFeatures();

      if(feats.length > 0) {
        String baseTag = feats[0];
        
        if(baseTag.startsWith("DT"))
          mFeats.addFeature(MorphoFeatureType.DEF, defVals[0]);
      }
      
      for(int i = 1; i < feats.length; i++) {
        String[] keyValue = feats[i].split(KEY_VAL_DELIM);
        if(keyValue.length != 2)
          continue;
        MorphoFeatureType fName = MorphoFeatureType.valueOf(keyValue[0].trim());
        mFeats.addFeature(fName, keyValue[1].trim());
      }
      return mFeats;
    }
    
    @Override
    public String getTag(String baseTag) {
      StringBuilder sb = new StringBuilder();
      
      if(baseTag.startsWith("VB") && !baseTag.equals("VBG"))
        baseTag = "VB";
      
      for(MorphoFeatureType feat : MorphoFeatureType.values()) {
        if(hasFeature(feat)) {
          if(feat == MorphoFeatureType.DEF) {
            if(!baseTag.startsWith("DT"))
              baseTag = "DT" + baseTag; //For compatibility with older ATB code, including headfinder
          } else
            sb.append(String.format("-%s:%s",feat,fSpec.get(feat)));
        }
      }
      
      return baseTag + sb.toString();
    }
  }
  
  /**
   * For debugging. Converts a set of long tags (BAMA analyses as in the ATB) to their morpho
   * feature specification. The input file should have one long tag per line.
   * 
   * @param args
   */
  public static void main(String[] args) {
    if(args.length != 2) {
      System.err.printf("Usage: java %s filename feats%n", ArabicMorphoFeatureSpecification.class.getName());
      System.exit(-1);
    }
    
    MorphoFeatureSpecification fSpec = new ArabicMorphoFeatureSpecification();
    String[] feats = args[1].split(",");
    for(String feat : feats) {
      MorphoFeatureType fType = MorphoFeatureType.valueOf(feat);
      fSpec.activate(fType);
    }
    
    File fName = new File(args[0]);
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fName)));

      int nLine = 0;
      for(String line;(line = br.readLine()) != null; nLine++) {
        MorphoFeatures mFeats = fSpec.strToFeatures(line.trim());
        System.out.printf("%s\t%s%n", line.trim(),mFeats.toString());
      }
      br.close();
      System.out.printf("%nRead %d lines%n",nLine);
      
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
