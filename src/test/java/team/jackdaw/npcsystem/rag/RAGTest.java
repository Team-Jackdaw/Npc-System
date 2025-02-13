package team.jackdaw.npcsystem.rag;

import org.junit.jupiter.api.Test;
import team.jackdaw.npcsystem.api.Ollama;

import java.util.List;

public class RAGTest {
    private static final WeaviateDB db = new WeaviateDB("http", "jackdaw-v3:8080");

    static {
        Ollama.API_URL = "http://192.168.122.74:11434";
    }

    @Test
    public void testBackground() {
        RAG.CHUNK_SIZE = 150;
        String text = """
                Background:
                A coup d'état in the Scott Empire saw the Prince seize power and murder the old King's family, leaving only the youngest of the young royals (Reb Alexander Lee) to escape on a ship. The mutineers silenced anyone who knew about the events of that year, falsely claiming to the public that the king had been assassinated. After years of displacement, Reb attends the University of Skeet in the city of Skeet, the capital of the Scottish Empire, as a foreign student. He met Mr. Tony at the university, and was introduced by Mr. Tony to join a fraternity organization at Helena's Tavern. And he also explores the mystery of his birth in Sketch City.
                                
                Characters:
                1. royalty:
                a) Aaron Alexandria Lee, Aaron Alexandria Lee (current King)
                i. Background Information: A former Prince of the Scott Empire and uncle to Ribo, he seized power through a coup and became the new King. ii. He is an ambitious and resourceful man who is able to execute his policies decisively and ruthlessly. Aaron Lee's strong military development and use of experimental weapons gave the empire an advantage in wars with its neighbors. Despite his consolidation of power, his style of rule aroused opposition from some of the people and nobles. ii.
                ii. He was a member of the radical faction of the “Shadow Council” and thought that he himself was in control of the “Shadow Council”, but in reality his influence was limited and he was actually a pawn of the “Shadow Council”. iii. In fact, he is a pawn of the “sub-council”.
                b) Ethan Lee, Ethan Lee (Prince)
                i. Background Information: A daughter of the King's sidekick, created as the new Prince. Ethan Lee is a resourceful and powerful young man who is trusted by the King. He appears to be loyal to the King, but in reality he is thinking and seeking possible changes to the way the King rules. ii.
                He is a member of the conservative “Shadow Council” under the leadership of Nicholas Houghton, which is unknown to the King.
                c) Adam Lee (son of Adam Lee)
                i. Background information: The King's first son, who should have been a Prince, but was marginalized and later placed under house arrest because he did not agree with the King on certain key issues. ii. Adam Lee has some support in the royal family. He is a gentle and thoughtful man who wants to rule the country in a more peaceful and rational way. ii.
                ii. He was aware of the “shadow parliament” but did not join it, believing that it would do the empire no good. iii.
                d) Edward Lee, Edward Lee (Former King)
                i. Background Information: The former King, leader of the Scottish Empire, was usurped by his own daughter, the current King, Aaron Lee. Edward Lee was a wise and compassionate ruler who tried to solve his country's problems through diplomacy and negotiation, but ultimately lost his life in the coup. iii.
                He was aware of the “Shadow Parliament” and was always in contact with the conservative Nicholas Houghton and had no knowledge of the radicals, so he did not receive any news at the time of the coup d'état.
                e) Ribo Lee, Lee Ribo (Former Crown Prince)
                i. Background Information: Former crown prince, fled overseas after the coup and was the only surviving child of King Edward Lee. After years of seclusion and growth, he has become more mature and decisive. Suspecting that the current king may have used black power to help him usurp the throne, Lee is planning how to uncover the truth and reclaim the throne. ii.
                ii. At the time of his escape and until now, he has no knowledge of the “Shadow Council”. 2.
                2. Members of the Cabinet:
                a) Alexander Graham (Finance Minister)
                i. Rank: Minister of Economic Affairs, responsible for the economic policy and financial management of the Empire. ii. Background: A businessman by profession, with a strong background in economics and finance. iii.
                Background: Born in the business world and known for his shrewd financial strategies and deep understanding of the economy. Actually a conservative member of the “shadow parliament”. iii.
                iii. Political Establishment: Supports market liberalization and advocates stimulating investment to boost the economy. iv.
                iv. Residence: Cabinet Apartment A; a grand mansion outside the castle in a private park surrounded by heavy security. b) Alicia's residence in the city of Bordeaux.
                (b) Alicia Corte (Diplomatic University)
                i. Rank: Minister for Foreign Affairs, responsible for international relations and foreign policy. ii.
                Background: Comes from a family with a long tradition of diplomacy and is known for its calmness and excellent negotiating skills. Practically a member of the radical wing of the “shadow parliament”. iii.
                Political Establishment: Favored an aggressive diplomatic strategy, strengthening alliances and cooperation with other nations. At the same time, promoted wars between the Empire and neighboring countries. iv.
                iv. Residence: Cabinet Apartment B; Cort's residence was a classical villa in a high-class residential area of the city, with a beautifully decorated interior and a personal bodyguard. c) Gracchus Graeme, a member of the Council of Europe, was a member of the Council of Europe.
                (c) Mr. Graham Gray (Defence Minister)
                i. Rank: Defense Minister, responsible for the country's military affairs and defense policy. ii. Background: Former military man.
                Background: Former military man, known for his firmness and decisiveness. Commanded the foreign wars of the Empire with the support of the Emperor and the Diplomatic University. iii. Political Establishment: The political establishment.
                iii. Political Establishment: Supported the strengthening of the military to ensure the security and sovereignty of the state. iv.
                iv. Residence: Cabinet Apartment D; Feldman resides in a relatively low-key family home in a quiet suburb in a peaceful setting, but still with professional security men patrolling the perimeter. viii.
                d) Nicholas Houghton (Royal Advisor to the Queen)
                i. Designation: Two-Dynasty President, Consultant, Open-ended, has played an important role during the reigns of both the former and the current King. A person who is old and dignified in manner, and who dresses neatly and in the manner of a traditional nobleman. He is both prudent and intelligent, with a deep understanding of politics and history. ii.
                Background: Well versed in the workings of politics and perceived to be a power player behind the scenes. In reality, he is the leader of the conservative “Shadow Council”, which controls half of the Empire's economy, and is rich beyond its means. iii. Political Establishment: A skilled politician, with a strong background in politics and history. iv.
                Political Establishment: Skilled at balancing the interests of different factions and seen as a bridge between conservatives and reformists in the political arena. During the reign of the king, he maneuvered between the royal family and the “shadow parliament” and did not support the decision of the radicals to overthrow the king. However, during the coup d'état, he remained silent. He wanted to weaken the power of the radicals, who were led by the new king. However, he remained loyal to the royal family. iv.
                iv. Special information: He was aware of the members of the Cabinet; he knew the general position of his first wife; he had the right to organize the affairs of the Cabinet. v. Address: Cabinet apartment.
                v. Address: Cabinet Apartment C; external address unknown.
                e) Sarah Feldman (Home Office)
                i. Rank: Minister of Public Service, responsible for domestic policy and public welfare. ii.
                ii. Background: Extensive experience in public service and popularity with the public. iii.
                iii. Political Establishment: Concerned with social equality and public rights, and supportive of government investment in education and health. iv. Dissatisfaction with the authoritarian political style of the current King. iv.
                iv) Residence: Cabinet Apartment E; Feldman lives in a relatively low-key family home in a quiet suburb in a peaceful environment, but still surrounded by professional security patrols. e) Jonathan Grisley's house is located in the heart of the city. f) Jonathan Grisley's house is located in the heart of the city. g) Jonathan Grisley's house is located in the heart of the city.
                (f) Jonathan Griffiths (Late Former Minister of Finance)
                i. Situation: Deceased, former Finance Minister. Prior to his death, he lived in a magnificent manor house in the eastern part of the city of Scott. ii.
                ii. Remarks: The estate was currently inherited by the family and may contain some papers and relics from the Grievous' tenure of office.
                g) Isabella Molins (Formerly Retired Diplomatic University)
                i. Status: Retired, former Minister for Foreign Affairs. She now lives in an elegant house on the outskirts of the city. ii.
                ii. Remarks: Moorings is a lover of art and literature and has a deep affection for the Royal Family.
                (h) Martin Reynolds (retired from the Defense University)
                i. Situation: Retired, former Defense Minister. He now lives in a quiet little town near the sea. ii.
                ii. Note: Reynolds, although retired, may still be in contact with the military.
                i) Alan Harthouse (Late Former Secretary of State for the Interior)
                i. Situation: Deceased, former Secretary of State for the Interior. The Hastings family was located in a district of the city, and his private library may contain valuable information. He was killed by the “shadow parliament” in the coup d'état for not cooperating. ii.
                Note: Harthaus was known for his rigorous work and concern for national security. 3.
                3. Members of the Shadow Council:
                a) Dr. Aleksandr Pesk (Finance Minister) (Conservative)
                b) Julia Colt (Foreign Affairs) (Radical)
                c) Nicholas Houghton (Royal Advisor University) (Conservative)
                d) Aaron Alexandria Lee, Aaron Alexandria University Lee (Current King) (Radical)
                e) Ethan Lee, Ethan Lee (Prince) (Conservative)
                f) Some noblemen
                g) Some businessmen
                h) Other unknown members
                4. Fraternity members:
                a) Ribo Lee (protagonist), Ribo Lee, who has just arrived in Skagit City, befriends Mr. Tony and joins the Brotherhood.
                b) Sensei Tony, ostensibly a teacher in the Alchemy Department at Scott University, is actually a member of the Brotherhood and is investigating the Shadow Council.
                c) Aeneas, Lee Rebel's girlfriend, is a friend of Lee Rebel's from his time in the Kaihei Islands.
                d) Other unknown members
                                
                Organizations:
                1) The Scott Empire:
                - The Empire is at war with its neighbors, but is currently at an overwhelming advantage due to the strength of the Skelt Empire's soldiers, especially since the new king came to power and sent out many experimental weapons.
                - The economy and politics of the empire are controlled by a few people.
                - Because of the war and heavy taxation, the people started to protest.
                - It was supported by a shadow parliament.
                2. The Imperial Cabinet:
                - A few members of the cabinet were secret members of the “Shadow Council” who manipulated decisions within the government.
                - Some members of the Cabinet became dissatisfied with the way the present King ruled and considered him too autocratic.
                - There was a power struggle within the Cabinet, with some members attempting to increase their own influence in order to position themselves for a possible change of power in the future.
                3. The Imperial Family:
                - The current king has consolidated his power, but his rule is opposed by some of the population and the nobility. He tries to maintain his position by revamping and strengthening security measures.
                - The king's son, who was apparently loyal to his father, was in fact suspicious of his way of ruling and secretly sought a change. As a result, the king created a supportive son as his Prince.
                - There were different factions and conflicting interests among the members of the royal family, and these disputes were kept strictly out of sight.
                4. Shadow Council:
                - This organization consists of a group of powerful people and businessmen as well as mercenaries. Each of the main members is an oligarch or a person in power who seeks political influence and increased wealth, even some members of the royal family.
                - The “Shadow Council” uses its network to secretly eliminate anyone who violates its interests, including assassination, opinion manipulation, and bribery of high-ranking officials.
                - In practice, it wields a great deal of political power behind the scenes, particularly by influencing government decisions through the manipulation of key cabinet members.
                - The organization financed the Imperial Laboratory by removing it from the control of the Imperial Household and the Cabinet and conducting prohibited experiments.
                - It is currently divided into radicals, who are in a position of strength after the overthrow of the King, and conservatives, who are relatively silent.
                - The headquarters is on the second floor of the Chamber of Commerce building next to Skeet Castle, where the Shadow Councillors often gather for meetings.
                5. Imperial Laboratory:
                - Backed by the Shadow Council, it has secret dealings with many of the oligarchs in the empire.
                - It is in the inner city of the palace, exposed as a spherical glass-roofed building, but the underground part of the building goes deeper, with the Cabinet having access to the first two underground floors, and members of the royal family having access to the third and fourth underground floors, but there are many more floors below, rumored to lead to the dungeon, and through the middle of it, through the lush cavernous area.
                - The main products include “potent potions” to control and neutralize opponents, “super warriors” for use in wars backed by the royal family, and “mufflers” to create barriers to prevent chatter from being overheard by others. Muffler": creates a barrier to prevent chat from being overheard.
                6. The Brotherhood:
                - Direct rivals of the Shadow Council, investigating and dismantling the Shadow Council's rule over the Empire.
                - Most members have their own apparent identities and do not know each other.
                - Possesses a large number of assassins.
                - Disgruntled with the rule of the current king.
                - Headquartered in Helena's Tavern, where assassination missions are distributed and information is exchanged.
                                
                """;
        try {
            RAG.record(db, text, "Document");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testQuery() {
        String text = """
                Who is king of the empire currently? What is his relation to Ribo?
                """;
        try {
            List<String> res = RAG.query(db, text, 3, "Document");
            System.out.println(res);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testQuestion() {
        String question = """
                Who is the king of the empire currently? How he came to power?
                """;
        try {
            String res = RAG.completion(db, question, 3, "Document");
            System.out.println(res);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
