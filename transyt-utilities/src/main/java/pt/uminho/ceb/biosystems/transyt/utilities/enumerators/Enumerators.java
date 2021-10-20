package pt.uminho.ceb.biosystems.transyt.utilities.enumerators;

public class Enumerators {

	public enum TypeOfTransporter {

		Symport,
		Antiport,
		Uniport,
		Biochemical,
		BiochemicalCoA,
		BiochemicalATP,
		BiochemicalGTP,
		RedoxNADH,
		Redox,
		PEPdependent,
		Default,
		Light,
		Sensor,
		MoreThanOne;

		public static String getTransportTypeAbb(TypeOfTransporter type){

			switch(type) {

			case Symport:
				return "Sym";
			case Antiport:
				return "Ant";
			case Uniport:
				return "Uni";
			case BiochemicalATP:
				return "Abc";
			case BiochemicalGTP:
				return "Abc";
			case BiochemicalCoA:
				return "CoA";
			case RedoxNADH:
				return "Nad";
			case Redox:
				return "Rdx";
			case PEPdependent:
				return "Pts";
			case Light:
				return "Lux";

			default:
				return "Bq";
			}
		}

		public static Integer getTransportTypeID(TypeOfTransporter type){

			switch(type) {

			case Uniport:
				return 0;
			case Symport:
				return 1;
			case Antiport:
				return 2;
			case BiochemicalATP:
				return 3;
			case BiochemicalGTP:
				return 3;
			case PEPdependent:
				return 4;
			case BiochemicalCoA:
				return 5;
			case RedoxNADH:
				return 6;
			case Redox:
				return 6;
			case Light:
				return 7;

			default:
				return 9;
			}
		}
		
		public static Integer getEnergyTransportID(TypeOfTransporter type, int reactantsSize) {
			
			switch(type) {
			
			case BiochemicalATP:
				
				if(reactantsSize > 3)
					return 1;
				
				return 0;
			case BiochemicalGTP:
				
				if(reactantsSize > 3)
					return 6;
				
				return 5;
			
			default:
				return 9;
			}
			
		}
	}
	
	public enum CoTransportedCompound {
		
		H,
		Na,
		K,
		Ca,
		Mg,
		Cl,
		Zn,
		Fe;
		
		public static CoTransportedCompound getEnumIfCotransportedCompound(String compound){

			if(compound.matches("^[Hh]\\d*[+-]*$"))
				return H;
			else if(compound.matches("^[Nn]a\\d*[+-]*$"))
				return Na;
			else if(compound.matches("^[Kk]\\d*[+-]*$"))
				return K;
			else if(compound.matches("^[Cc]a\\d*[+-]*$"))
				return Ca;
			else if(compound.matches("^[Mm]g\\d*[+-]*$"))
				return Mg;
			else if(compound.matches("^[Cc]l\\d*[+-]*$"))
				return Cl;
			else if(compound.matches("^[Zz]n\\d*[+-]*$"))
				return Zn;
			else if(compound.matches("^[Ff]e\\d*[+-]*$"))
				return Fe;
			
			return null;
		}
		
		public static Integer getTransportTypeID(CoTransportedCompound type){
			
			if(type == null)
				return 9;

			switch(type) {				//de 100.000 em 100.000 trocar de categoria -> seguir a numeração do model SEED

			case H:
				return 0;
			case Na:
				return 1;
			case K:
				return 2;
			case Ca:
				return 3;
			case Mg:
				return 4;
			case Cl:
				return 5;
			case Zn:
				return 6;
			case Fe:
				return 7;
			
			default:
				return 9;
			}
		}
	}
	
	public enum Direction {
		
		R,
		O,
		I,
		X,
		Y,
		Z,
		U;
		
		public static Direction getDirection(Boolean isReversible, Boolean outToIn){
			
			if(isReversible == null)
				return Z; 
			
			if(!isReversible && outToIn == null)
				return Y;
			
			if(isReversible && outToIn == null)
				return X;
			
			if(isReversible)
				return R;
			
			if(!isReversible && !outToIn)
				return I;
			
			if(!isReversible && outToIn)
				return O;
			
			return Z;
			
		}
	}
	
	public enum KINGDOM{
		//TODO Bacteria(Bacteria)
		Bacteria ("Bacteria"),
		Eukaryota ("Eukaryota"),
		Archaea ("Archaea"),
		Viruses ("Viruses"),
		Viroids ("Viroids"),
		All("All");
		
		private String kingdom;
		
		private  KINGDOM (String kingDom) {
			
			this.kingdom = kingDom;
		}
		
		public String getKingdom(){
			return this.kingdom;
		}
	}

	public enum STAIN {

		gram_positive,
		gram_negative
	}

}
