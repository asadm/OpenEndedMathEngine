import java.io.IOException;
import java.util.ArrayList;

public class reasoner {
		ArrayList<formula> KB;
		node exp;
		ArrayList<formula> AFL;
		ArrayList<String> steps;
		FileWriter1 fw;
		ArrayList<GoalNode> TopLevelGoals = new ArrayList<GoalNode>();
		ArrayList<String> Known;
		ArrayList<String> KnownValues;
		ArrayList<formula_static> KB2;
		
		private static final reasoner inst = new reasoner();
		
		public static reasoner instance()
		{
			return inst;
		}
				
		private reasoner()
		{
			//TODO: make provision to load kb from file 
			KB = new ArrayList<formula>();
			KB2 = new ArrayList<formula_static>();
			steps = new ArrayList<String>();
			AFL = new ArrayList<formula>();
			Known = new ArrayList<String>();
			KnownValues = new ArrayList<String>();
			//fw=new FileWriter1("kb.txt");
		}
		
		
		public void learnFormula(String state1, String state2, ArrayList<String> kw)
		{
			KB.add(new formula(state1, state2, kw));
		}
		
		public void learnFormulaStatic(String str)
		{
			KB2.add(new formula_static(str));
		}
		
		String getKnown(String var)
		{
			return KnownValues.get( Known.indexOf(var) );
		}
		
		public node TransformationalQuery(node n, ArrayList<String> kw)
		{
			exp = n;
			steps.clear();
			
			do
			{
				AFL.clear();
				steps.add(exp.infix());
				int max = 0;
				
				for(int i = 0; i<KB.size(); i++)
				{
					if(KB.get(i).q_simplify(exp))		//if it is applicable
					{
						int matches = 0;
						for(int j=0; j< kw.size(); j++)		//count how many keywords match for this formula
						{
							for(int k=0; k<KB.get(i).keywords.size(); k++)
							{
								if(kw.get(j).equals(KB.get(i).keywords.get(k)) )
								{
									matches++;
								}
							}
						}
				//		System.out.println(KB.get(i).isRecursive());
						if(KB.get(i).isRecursive())
						{
							AFL.add(KB.get(i));			//add recursive formula at end (least priority)
						}
						else if(matches>=max)		//if max keyword matches (best option)
						{
							AFL.add(0, KB.get(i));		//insert at first position
							max = matches;				//update max
						}
						else
							AFL.add(KB.get(i));			//else insert at end
					}
				}
				
				//apply best match formula
				if(AFL.size() > 0)
				{
					exp = AFL.get(0).simplify(exp);
					System.out.println(exp.dfs2());
				}
			//	System.out.println(exp.infix());
			}while(AFL.size() > 0);
			
			exp.simplify_solve();
			return exp;
		}
		
		
		//TODO: make destructor which saves KB in file
		
		
		boolean usableFormulaStatic(formula_static f)		//can we use this formula?
		{
			for(String l: f.Reqs)							//do we know all the reqs?
			{
				if(!Known.contains(l))
					return false;
			}
			return true;
		}
		
		void replaceKnown(node n)				//put in known values
		{
			if(Known.contains(n.data))			//if the data is known,
			{
				//get index of the variable in Known, and replace with corresponding KnownValue
				node t = new node(new String( KnownValues.get(Known.indexOf(n.data)) ));		
				n.data = t.data;
				n.child.addAll(t.child);
			}
			for(node a: n.child)
			{
				replaceKnown(a);
			}
		}
		
		void evaluate(formula_static f)			//evaluate formula to add result to known
		{
			if(!usableFormulaStatic(f))
			{
				return;
			}
			node temp = new node(f.RHS);		//make temporary copy of formula
			replaceKnown(temp);					//put in known values
			Known.add(f.Result);	
			//add result to our knowledge base
			ArrayList<String> kw = new ArrayList<String>();
			KnownValues.add( TransformationalQuery(temp,kw).infix() );	
		}
		
		
		
		ArrayList<String> KnowledgeQuery(ArrayList<String> GivenVars, ArrayList<String> GivenValues, ArrayList<String> ToFind)
		{
			
			//TODO: Make sure if not found, returns error
			
			steps.clear();
			Known.clear();
			Known.addAll(GivenVars);
			KnownValues.clear();
			KnownValues.addAll(GivenValues);
		//	fw.SaveKB(KB2.toArray().toString(), "kb2.txt");
	//		fw.SaveKB(KB.toArray().toString(), "kb1.txt");
			TopLevelGoals.clear();
			for(String str: ToFind)
			{
				TopLevelGoals.add( new GoalNode(str));
			}
			
			ArrayList<String> Answers = new ArrayList<String>();
			
			
			for(GoalNode g: TopLevelGoals)
			{
				Achieve(g);
				Answers.add( KnownValues.get(Known.indexOf(g.tag)) );
			}
			
			
			return Answers;
		}
		
		int unknown(formula_static f)		//number of unknown reqs
		{
			int count = 0;
			for(String r: f.Reqs)
			{
				if(!Known.contains(r))
					count++;
			}
			return count;
		}
		
		public void PrintSteps(ArrayList<String> solSteps)
		{
			for(GoalNode g: TopLevelGoals)
			{
				PrintSteps(g,solSteps);
			}
		}
		
		public void PrintSteps(String str, ArrayList<String> solSteps)
		{
			for(GoalNode g: TopLevelGoals)
			{
				if(str.equals(g.tag))
					PrintSteps(g,solSteps);
			}
		}
		
		private void PrintSteps(GoalNode g, ArrayList<String> solSteps)
		{
			for( GoalNode g2: g.chosenPlan.subgoals)
			{
				PrintSteps(g2,solSteps);
			}
			System.out.println( g.chosenPlan.action.formulaStr);
			node temp = new node(g.chosenPlan.action.RHS );
			replaceKnown(temp);
			solSteps.add("= "+ temp.infix());
			solSteps.add("= "+ getKnown(g.tag));
			solSteps.add("");
		}
		
		
		
		void Achieve(GoalNode g)
		{
			
			for(formula_static f: KB2)
			{
				if(usableFormulaStatic(f) && f.Result.equals(g.tag))	//if perfect, apply
				{
					g.chosenPlan = new plan(f);
					evaluate(f);
					g.achieved = true;
					return;
				}
				else if(f.Result.equals(g.tag))		//add applicable plans to APL
				{
					g.APL.add(new plan(f));
				}
			}
			 
			for(int i =0 ; i<g.APL.size(); i++)		//sort by number of unknowns
			{
				int min = i;
				for(int j = i+1; j<g.APL.size();j++)
				{
					if(unknown(g.APL.get(min).action) > unknown(g.APL.get(j).action))
					{
						min = j;
					}
				}
				plan temp = g.APL.get(min);			//swapping
				g.APL.set(min, g.APL.get(i));
				g.APL.set(i, temp);
			}
			
			//TODO: Create safety mechanism that insures against infinite looping
			
			int depth = 0;
			while(!g.achieved)			//try APL with increasing depth
			{
				for(plan p : g.APL)
				{
					if(tryPlan(p, depth))
					{
						g.chosenPlan = p;
						g.achieved = true;
						break;
					}
				}
				depth++;
			}
		}
		
		boolean tryPlan(plan p, int depth)		//try plan within given depth
		{
			if(depth < 0)						
				return false;
			
			if(usableFormulaStatic(p.action))		//if all reqs are known, apply formula
			{
				evaluate(p.action);
				return true;
			}
			else
			{
				if(p.subgoals.size() == 0)		//no subgoals, make em
				{
					for(String r: p.action.Reqs)	//for each unknown, a subgoal
					{
						if(!Known.contains(r))
						{
							p.subgoals.add(new GoalNode(r));
						}
					}
				}
				
				int achieved = 0;					//counts how many achieved
				for(GoalNode g: p.subgoals)
				{
					if(g.achieved)
						achieved++;
					else if(trySubGoal(g, depth-1))		//try unachieved subgoals (with one less depth)
						achieved++;
				}
				
				if(achieved == p.subgoals.size())
				{
					evaluate(p.action);
					return true;
				}
				else
					return false;
			}
		}
		
		boolean trySubGoal(GoalNode g, int depth)
		{
			if(depth < 0)
				return false;
			
			if(g.APL.size() == 0)
			{
				for(formula_static f: KB2)
				{
					if(usableFormulaStatic(f) && f.Result.equals(g.tag))	//if perfect, apply
					{
						g.chosenPlan = new plan(f);
						evaluate(f);
						g.achieved = true;
						return true;
					}
					else if(f.Result.equals(g.tag))		//add applicable plans to APL
					{
						g.APL.add(new plan(f));
					}
				}
			}
			 
			for(int i =0 ; i<g.APL.size(); i++)		//sort by number of unknowns
			{
				int min = i;
				for(int j = i+1; j<g.APL.size();j++)
				{
					if(unknown(g.APL.get(min).action) > unknown(g.APL.get(j).action))
					{
						min = j;
					}
				}
				plan temp = g.APL.get(min);			//swapping
				g.APL.set(min, g.APL.get(i));
				g.APL.set(i, temp);
			}
			
			for(plan p : g.APL)
			{
				if(tryPlan(p, depth-1))
				{
					g.chosenPlan = p;
					g.achieved = true;
					break;
				}
			}
			
			if(g.achieved)
				return true;
			else
				return false;
		}
		
		public void rSaveKB(String list, String s)
		{
			fw.SaveKB(list,s);
		}
	
}
