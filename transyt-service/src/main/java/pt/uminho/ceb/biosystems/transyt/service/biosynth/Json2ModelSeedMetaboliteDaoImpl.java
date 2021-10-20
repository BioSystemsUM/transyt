package pt.uminho.ceb.biosystems.transyt.service.biosynth;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;

import pt.uminho.sysbio.biosynthframework.ReferenceType;
import pt.uminho.sysbio.biosynthframework.biodb.seed.ModelSeedMetaboliteCrossreferenceEntity;
import pt.uminho.sysbio.biosynthframework.biodb.seed.ModelSeedMetaboliteEntity;
import pt.uminho.sysbio.biosynthframework.io.MetaboliteDao;

public class Json2ModelSeedMetaboliteDaoImpl implements MetaboliteDao<ModelSeedMetaboliteEntity> {

  private static final Logger logger = LoggerFactory.getLogger(Json2ModelSeedMetaboliteDaoImpl.class);
  
  private Map<String, ModelSeed2Compound> data = new HashMap<> ();
  
  public static Boolean integerToBoolean(Integer i) {
    if ( i == null) {
      return null;
    }
    return i == 0 ? false : true;
  }
  
  public Function<ModelSeed2Compound, ModelSeedMetaboliteEntity> function =
      new Function<ModelSeed2Compound, ModelSeedMetaboliteEntity>() {

        @Override
        public ModelSeedMetaboliteEntity apply(ModelSeed2Compound t) {
          ModelSeedMetaboliteEntity cpd = new ModelSeedMetaboliteEntity();
          cpd.setEntry(t.id);
          cpd.setName(t.name);
          cpd.setFormula(t.formula);
          
          cpd.setAbbreviation(t.abbreviation);
          cpd.setSeedSource(t.source);
          
          cpd.setSearchInchi(t.search_inchi);
          cpd.setStructure(t.structure);
          
          cpd.setCore(integerToBoolean(t.is_core));
          cpd.setCofactor(integerToBoolean(t.is_cofactor));
          cpd.setObsolete(integerToBoolean(t.is_obsolete));
          
          cpd.setDefaultCharge(t.charge);
          cpd.setDeltaG(t.deltag);
          cpd.setDeltaGErr(t.deltagerr);
          
          cpd.setMass(t.mass);
          
          //probably should leave the transformer to fix it ...
          List<ModelSeedMetaboliteCrossreferenceEntity> refs = new ArrayList<> ();
          for (String bigg : t.bigg_aliases) {
            refs.add(new ModelSeedMetaboliteCrossreferenceEntity(
                ReferenceType.DATABASE, "BiGG", bigg));
          }
          for (String kegg : t.kegg_aliases) {
            String database = null;
            switch (kegg.charAt(0)) {
              case 'C': database = "LigandCompound"; break;
              case 'G': database = "LigandGlycan"; break;
              default:
                logger.warn("unknown KEGG database - {}", kegg);
                break;
            }
            
            if (database != null) {
              refs.add(new ModelSeedMetaboliteCrossreferenceEntity(
                  ReferenceType.DATABASE, database, kegg));
            }
          }
          for (String metacyc : t.metacyc_aliases) {
            refs.add(new ModelSeedMetaboliteCrossreferenceEntity(
                ReferenceType.DATABASE, "MetaCyc", "META:" + metacyc));
          }
          
          cpd.setCrossreferences(refs);
          
          for (String name : t.names) {
            cpd.getNames().add(name);
          }
          
          return cpd;
        }
      };
  
  public Json2ModelSeedMetaboliteDaoImpl(Resource compoundsJson) {
    ObjectMapper m = new ObjectMapper();
    m.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    MapType ctype = m.getTypeFactory()
                            .constructMapType(HashMap.class, String.class, 
                            		ModelSeed2Compound.class);

    try {
      Map<String, ModelSeed2Compound> compounds = m.readValue(compoundsJson.getInputStream(), ctype);
      for (String id : compounds.keySet()) {
    	  ModelSeed2Compound cpd = compounds.get(id);
        if (cpd != null && cpd.id != null && !cpd.id.trim().isEmpty()) {
          if (data.put(cpd.id, cpd) != null) {
            logger.warn("duplicate ID - {}", cpd.id);
          }
        } else {
          logger.warn("invalid record - {}", cpd);
        }
      }
    } catch (IOException e) {
      logger.error("IO Error: {}", e.getMessage());
    }
  }
  
  @Override
  public ModelSeedMetaboliteEntity getMetaboliteById(Serializable id) {
    throw new RuntimeException("Unsupported operation");
  }

  @Override
  public ModelSeedMetaboliteEntity getMetaboliteByEntry(String entry) {
    return function.apply(data.get(entry));
  }

  @Override
  public ModelSeedMetaboliteEntity saveMetabolite(ModelSeedMetaboliteEntity metabolite) {
    throw new RuntimeException("Unsupported operation");
  }

  @Override
  public Serializable saveMetabolite(Object metabolite) {
    throw new RuntimeException("Unsupported operation");
  }

  @Override
  public List<Serializable> getAllMetaboliteIds() {
    throw new RuntimeException("Unsupported operation");
  }

  @Override
  public List<String> getAllMetaboliteEntries() {
    return new ArrayList<> (this.data.keySet());
  }

  @Override
  public Serializable save(ModelSeedMetaboliteEntity entity) {
    throw new RuntimeException("Unsupported operation");
  }
  
}
