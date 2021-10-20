package pt.uminho.ceb.biosystems.transyt.service.biosynth;

import java.util.ArrayList;
import java.util.List;

public class ModelSeed2Compound {
	  
//  "aliases": "null", 
//  "charge": "0", 
//  "comprised_of": "null", 
//  "deltag": "-56.687", 
//  "deltagerr": "0.5", 
//  "formula": "H2O", 
//  "id": "cpd00001", 
//  "is_cofactor": 0, 
//  "is_core": 1, 
//  "is_obsolete": 0, 
//  "linked_compound": "null", 
//  "mass": "18", 
//  "name": "H2O", 
//  "pka": "1:15.7", 
//  "pkb": "1:-1.8", 
//  "source": "ModelSEED", 
//  "structure": "InChI=1S/H2O/h1H2"
  public String abstract_compound;
  
  public String source;
  public String id;
  public String name;

  public String formula;
  
  public Integer charge;
  public Double deltag;
  public Double deltagerr;
  
  
  public String search_inchi;
  public String structure;
  public Double mass;
  
  
  public String abbreviation;
  
  public Integer is_cofactor;
  public Integer is_core;
  public Integer is_obsolete;
  
  public List<String> names = new ArrayList<> ();
  public List<String> metacyc_aliases = new ArrayList<> ();
  public List<String> searchnames = new ArrayList<> ();
  public List<String> bigg_aliases = new ArrayList<> ();
  public List<String> kegg_aliases = new ArrayList<> ();
  
  public String pka = new String();
  public String pkb = new String();
  public String groups = new String();
  
  @Override
  public String toString() {
    return String.format("[%s]%s %s %s:%s", source, id, formula, abbreviation, name);
  }
}
