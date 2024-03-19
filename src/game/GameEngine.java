package game;

import players.*;

import java.util.*;

//updated 20240319 08:59
public class GameEngine {
    // The speed at which the game progresses, 0 for full speed.
    private int gameSpeed;

    // The deck of cards used in the game.
    private Deck deck;

    // Cards that are currently on the table (community cards in poker).
    private List<Card> tableCards;

    // List of players still in the game.
    private List<Player> listPlayersRemainingGame;
    // List of players still in the current round.
    private List<Player> listPlayersRemainingRound;
    // List of players who have won the game.
    private List<Player> listPlayersWinner;
    // Mapping of player names to their bank balances.
    private List<Map<String, Integer>> listPlayersNameBankMap;

    // Countdown to next ante increase.
    private int tableAnteCountdown;
    // The small ante amount.
    private int tableAnteSmall;
    // The big ante amount.
    private int tableAnteBig;
    // The current pot amount.
    private int tablePot;
    // The current bet amount.
    private int tableBet;
    // The minimum bet amount, initially set to the big ante.
    private int tableMinBet;
    // The current raise amount.
    private int tableRaise;
    // Flag to indicate if there is an active bet.
    private boolean activeBet;
    // Number of players left to act after an active bet.
    private int activeBetNumberOfPlayersLeft;

    // Total number of games played.
    private int numTotalGames;
    // The current stage of the round (e.g., pre-flop, flop, turn, river).
    private int numRoundStage;

    // The dealer, small blind, and big blind players.
    private Player dealer;
    private Player small;
    private Player big;

    // Index of the dealer in the list of players.
    private int dealerIndex;

    // The current state of the game, containing all relevant information.
    private GameState state;

    // Static counter for the number of games played across instances.
    public static int numGamesPlayed = 0;

    /**
     * Constructor for the GameEngine class. Initializes the game with a set of players and game settings.
     */
    public GameEngine() {
        //0 (fullSpeed) to Integer.MAX_VALUE (~24 days)
        gameSpeed = 0;

        // Initialize the deck and shuffle it.
        deck = new Deck();
        deck.shuffle();

        // Initialize the list of table cards and player lists.
        tableCards = new ArrayList<>();
        listPlayersRemainingGame = new ArrayList<>();

        // Add NPC players to the game. These methods should be defined to add specific types of NPC players.

        //listPlayersRemainingGame.add(new ManualPlayer("manualPlayer"));
        listPlayersRemainingGame.add(new Anton("Anton"));
        addConservativeNPCs(2);
        addSimpleNPCs(2);
        addRandomNPCs(1);
        addTempPlayers(0);
        Collections.shuffle(listPlayersRemainingGame);

        // Initialize the lists for the current round, winners, and player bank mappings.
        listPlayersRemainingRound = new ArrayList<>(listPlayersRemainingGame);
        listPlayersWinner = new ArrayList<>();
        listPlayersNameBankMap = new ArrayList<>();

        // Set initial ante values and pot/bet amounts.
        tableAnteCountdown = listPlayersRemainingRound.size();
        tableAnteSmall = 2;
        tableAnteBig = tableAnteSmall * 2;
        tablePot = 0;
        tableBet = 0;
        tableRaise = 0;
        tableMinBet = tableAnteBig;
        activeBet = false;
        activeBetNumberOfPlayersLeft = 0;

        // Initialize game and round counters.
        numTotalGames = 0;
        numRoundStage = 0;
        dealerIndex = 0;

        // Create and update the game state for all players.
        state = new GameState(tableCards, listPlayersNameBankMap, deck.getDeckSize(), listPlayersRemainingGame.size(), listPlayersRemainingRound.size(), tableAnteCountdown, tableAnteSmall, tableAnteBig, tablePot, tableBet, tableMinBet, activeBet, activeBetNumberOfPlayersLeft, numTotalGames, numRoundStage, dealer, small, big, dealerIndex);
        for (Player tempPlayer : listPlayersRemainingRound) {
            tempPlayer.updateGameState(state);
        }
    }

    public void start() {
        // Display a welcome message at the beginning of the game.
        printWelcome();

        // Main game loop that continues until only one player remains.
        while(listPlayersRemainingGame.size() > 1) {
            // Pre-flop: The initial stage of the game where each player is dealt two cards.
            numRoundStage = 0; // Reset the round stage to pre-flop.
            rotateDealer(); // Rotate the dealer position.
            dealHoleCards(2); // Deal two hole cards to each player.
            printGameDetails(); // Print the current state of the game.
            betPhase(); // Execute the betting phase for the pre-flop.

            // Flop: The stage where three community cards are dealt.
            numRoundStage++; // Increment the round stage to indicate the flop.
            dealTableCards(3); // Deal three community cards.
            printGameDetails(); // Print the current state of the game.
            betPhase(); // Execute the betting phase for the flop.

            // Turn: The stage where one additional community card is dealt.
            numRoundStage++; // Increment the round stage to indicate the turn.
            dealTableCards(1); // Deal one additional community card.
            printGameDetails(); // Print the current state of the game.
            betPhase(); // Execute the betting phase for the turn.

            // River: The final stage where the last community card is dealt.
            numRoundStage++; // Increment the round stage to indicate the river.
            dealTableCards(1); // Deal the final community card.
            printGameDetails(); // Print the current state of the game.
            betPhase(); // Execute the final betting phase.
            showdownPhase(); // Determine the winner of the hand.

            // Reset the game state for a new hand.
            newHandReset();
        }
    }

    private void betPhase() {
        // Flag to indicate when the betting phase is complete.
        boolean phaseComplete = false;
        // Reset the current bet to 0 at the start of the betting phase.
        tableBet = 0;
        Player bettor = null;


        // Check if there is only one player remaining in the round.
        if(listPlayersRemainingRound.size() == 1) {
            System.out.println("###ONE PLAYER REMAINING, ADVANCING ROUND###\n");
            return; // End the betting phase early if only one player remains.
        }

        // Display the current pot and bet amounts.
        System.out.println("Current Pot: $" + tablePot + ", Current Bet: $" + tableBet);
        // Pause for a moment based on the game speed setting.
        sleep(gameSpeed);

        // Continue the betting phase until it is complete.
        while(!phaseComplete) {
            // List to track players who fold or are otherwise removed during this betting phase.
            List<Player> listPlayersToRemoveFromRound = new ArrayList<>();

            // Before each betting iteration, reset the count of players who have yet to act.
            int playersWithAction = 0;

            // Iterate over each player still in the round to take their action.
            for(Player tempPlayer: listPlayersRemainingRound) {
                if(!tempPlayer.isFold() && !tempPlayer.isAllIn()) {
                    playersWithAction++;
                }
            }

            // Check if all remaining players have gone all-in. If so, no further action is possible, and the phase can end.
            boolean allPlayersAllIn = true;
            for(Player tempPlayer: listPlayersRemainingRound) {
                if(!tempPlayer.isAllIn()) {
                    allPlayersAllIn = false;
                    break; // As soon as one player who is not all-in is found, break the loop.
                }
            }

            // If all players are all-in, end the betting phase early since no further bets can be placed.
            if(allPlayersAllIn) {
                return;
            }

            if(activeBet) {
                playersWithAction--;
            }

            // Iterate over a copy of the list of players still in the round to avoid concurrent modification issues.
            for(Player tempPlayer: new ArrayList<>(listPlayersRemainingRound)) {
                // Skip players who have already folded or are all-in, as they cannot take further actions.
                if(tempPlayer.isFold() || tempPlayer.isAllIn() || tempPlayer.equals(bettor)) continue;

                // Display the current player's name, bank balance, and hand cards.
                System.out.println("Action: " + tempPlayer.getName() + ", Bank: $" + tempPlayer.getBank());
                for(Card tempCard: tempPlayer.getHandCards()) {
                    System.out.print(tempCard.toString() + " ");
                }
                // Evaluate and display the player's current hand strength.
                System.out.println(tempPlayer.evaluatePlayerHand());
                // Pause for game speed setting before continuing.
                sleep(gameSpeed);
                // Display the current pot and bet amounts for context.
                System.out.println("Current Pot: $" + tablePot + ", Current Bet: $" + tableBet);
                sleep(gameSpeed);


                // Request the player's action for this round, providing the current game state as context.
                PlayerActions action = tempPlayer.getPlayerAction(new GameState(tableCards, listPlayersNameBankMap, deck.getDeckSize(), listPlayersRemainingGame.size(), listPlayersRemainingRound.size()-listPlayersToRemoveFromRound.size(), tableAnteCountdown, tableAnteSmall, tableAnteBig, tablePot, tableBet, tableMinBet, activeBet, activeBetNumberOfPlayersLeft, numTotalGames, numRoundStage, dealer, small, big, dealerIndex));
                // Handle null actions by either forcing a check if only one player remains or folding the player otherwise.
                if(action == null) {
                    if(listPlayersRemainingRound.size()-listPlayersToRemoveFromRound.size() == 1) {
                        System.out.println("###NULL PLAYER ACTION RECEIVED, ONE PLAYER LEFT, FORCING CHECK###");
                        action = PlayerActions.CHECK; // Force a check if only one player is left.
                    } else {
                        System.out.println("###NULL PLAYER ACTION RECEIVED, FORCING FOLD###");
                        tempPlayer.setIsFold(true);
                        action = PlayerActions.FOLD; // Fold the player if their action is null and more than one player remains.
                    }
                }

                // Process the player's action based on their decision.
                switch(action) {
                    case FOLD:
                        // Log the fold action and decrement the count of players with actions left.
                        System.out.println("###FOLD###");
                        playersWithAction--;
                        // Add the player to the list of players to remove from the round if they fold.
                        if(listPlayersToRemoveFromRound.size() < listPlayersRemainingRound.size()-1) {
                            listPlayersToRemoveFromRound.add(tempPlayer);
                        }

                        // Decrement the count of players left to act if there's an active bet.
                        if(activeBetNumberOfPlayersLeft > 0) {
                            activeBetNumberOfPlayersLeft--;
                        }

                        break;
                    case CHECK:
                        // Log the check action and decrement the count of players with actions left.
                        System.out.println("###CHECK###");
                        playersWithAction--;

                        break;
                    case CALL:
                        // Log the call action, mark the bet as active, and decrement the count of players with actions left.
                        System.out.println("###CALL###");
                        playersWithAction--;
                        activeBet = true;

                        // Adjust the player's bank for the call amount and add it to the pot.
                        tempPlayer.adjustPlayerBank(-tempPlayer.getBet());
                        tablePot += tempPlayer.getBet();

                        // Decrement the count of players left to act if there's an active bet.
                        if(activeBetNumberOfPlayersLeft > 0) {
                            activeBetNumberOfPlayersLeft--;
                        }

                        // Log the call amount and the player's new bank balance.
                        System.out.println("player call: $" + tempPlayer.getBet());
                        System.out.println("player bank: $" + tempPlayer.getBank());
                        break;
                    case RAISE:
                        // Log the raise action, mark the bet as active, and decrement the count of players with actions left.
                        System.out.println("###RAISE###");
                        playersWithAction--;
                        activeBet = true;
                        bettor = tempPlayer;


                        // Adjust the player's bank for the raise amount, update the pot and the current bet to match the raise.
                        tempPlayer.adjustPlayerBank(-tempPlayer.getBet());
                        tablePot += tempPlayer.getBet();
                        tableBet = tempPlayer.getBet();

                        // Reset the count of players left to act after the raise, excluding the raising player.
                        activeBetNumberOfPlayersLeft = listPlayersRemainingRound.size() - listPlayersToRemoveFromRound.size() - 1;

                        // Log the raise amount and the player's new bank balance.
                        System.out.println("player raise: $" + tempPlayer.getBet());
                        System.out.println("player bank: $" + tempPlayer.getBank());
                        break;
                    case ALL_IN:
                        // Log the all-in action, mark the bet as active, and decrement the count of players with actions left.
                        System.out.println("###ALL_IN###");
                        playersWithAction--;
                        activeBet = true;
                        bettor = tempPlayer;

                        // Adjust the player's bank for the all-in amount, update the pot and the current bet to match.
                        tempPlayer.adjustPlayerBank(-tempPlayer.getBet());
                        tablePot += tempPlayer.getBet();
                        tableBet = tempPlayer.getBet();

                        // Reset the count of players left to act after the all-in, excluding the all-in player.
                        activeBetNumberOfPlayersLeft = listPlayersRemainingRound.size() - listPlayersToRemoveFromRound.size() - 1;

                        // Log the all-in amount and the player's new bank balance.
                        System.out.println("player all in: $" + tempPlayer.getBet());
                        System.out.println("player bank: $" + tempPlayer.getBank());
                        break;
                }

                // Update the mapping of player names to their current bank balances after each action.
                updateListPlayerNameBankMap();

                // Pause for a moment to simulate real-time gameplay and allow for readability of the game's progress.
                sleep(gameSpeed);



                // If there are no players left with actions due to all players checking, calling, or being all-in, deactivate the active bet.
                if(activeBetNumberOfPlayersLeft == 0) {
                    activeBet = false;
                }

                System.out.println("activeBetNumberOfPlayersLeft " + activeBetNumberOfPlayersLeft);
                System.out.println("activeBet " + activeBet);
                System.out.println("playersWithAction " + playersWithAction);


                // Determine if the betting phase is complete. This occurs when all players have acted (no players with actions left) and there is no active bet.
                if(playersWithAction <= 0 && !activeBet) {
                    phaseComplete = true;
                }

                // Print a newline for better readability of the game's output in the console.
                System.out.println();
            }

            // Remove players who have folded or are otherwise not continuing in the round.
            // This is done only if the number of players to remove is less than the total number of players,
            // ensuring that at least one player remains in the round.
            if(listPlayersToRemoveFromRound.size() < listPlayersRemainingRound.size()) {
                listPlayersRemainingRound.removeAll(listPlayersToRemoveFromRound);
            }
        }
    }


    private void newHandReset() {
        // Remove players with a bank balance of zero or less, indicating bankruptcy.
        removeBankruptPlayers();

        // Reinitialize the deck and shuffle for a new hand.
        deck = new Deck();
        deck.shuffle();

        // Clear the table cards and reset pot and bet values for the new hand.
        tableCards.clear();
        tablePot = 0;
        tableBet = 0;

        // Increment the total number of games played.
        numTotalGames++;
        // Clear the list of players remaining in the round, preparing for the new hand.
        listPlayersRemainingRound.clear();

        // Rotate the dealer position by moving the first player in the list to the end.
        listPlayersRemainingGame.add(listPlayersRemainingGame.remove(0));

        // Re-add all remaining players to the round.
        for(Player tempPlayer: listPlayersRemainingGame) {
            listPlayersRemainingRound.add(tempPlayer);
        }
        // If only one player remains, declare them the final winner and restart the game.
        if(listPlayersRemainingRound.size() == 1) {
            System.out.println("FINAL WINNER: " + listPlayersRemainingRound.get(0).getName() + ", BANK: $" + listPlayersRemainingRound.get(0).getBank());
            System.out.println("Number of games simulated: " + ++numGamesPlayed);
            sleep(60000); // Pause before restarting the game.

            GameEngine game = new GameEngine(); // Create a new instance of the game.
            game.start(); // Start the new game instance.
            // Note: The system exit call is commented out to allow the game to restart.
            //System.exit(0);
        }

        // Reset each player for the new hand.
        for(Player tempPlayer: listPlayersRemainingRound) {
            tempPlayer.newHandReset();
        }

        // Clear and update the list of player name to bank balance mappings for the new hand.
        listPlayersNameBankMap.clear();
        for(Player tempPlayer: listPlayersRemainingRound) {
            Map<String, Integer> tempMap = new HashMap<>();
            tempMap.put(tempPlayer.getName(), tempPlayer.getBank());
            listPlayersNameBankMap.add(tempMap);
        }
    }

    private void updateListPlayerNameBankMap() {
        listPlayersNameBankMap = new ArrayList<>();
        for(Player tempPlayer: listPlayersRemainingRound) {
            Map<String, Integer> tempMap = new HashMap<>();
            tempMap.put(tempPlayer.getName(), tempPlayer.getBank());
            listPlayersNameBankMap.add(tempMap);
        }
    }

    private void removeBankruptPlayers() {
        for(Player tempPlayer: new ArrayList<>(listPlayersRemainingGame)) {
            if(tempPlayer.getBank() <= 0) {
                if(listPlayersRemainingGame.size() > 1) {
                    listPlayersRemainingGame.remove(tempPlayer);
                }
            }
        }
    }

    private void showdownPhase() {
        // Create an evaluator instance with the remaining players and table cards to find the winner(s).
        Evaluator evaluator = new Evaluator(listPlayersRemainingRound, tableCards);
        // Determine the winner(s) based on the best hand.
        listPlayersWinner = evaluator.findWinners();

        // Display the community cards on the table.
        for(Card tempCard: tableCards) {
            System.out.print(tempCard.toString() + " ");
        }
        sleep(gameSpeed); // Pause for readability.
        System.out.println("\n");

        // Handle the case of a single winner.
        if (listPlayersWinner.size() == 1) {
            // Award the pot to the sole winner.
            listPlayersWinner.get(0).adjustPlayerBank(tablePot);
            // Announce the winner and their winning hand.
            System.out.print("WINNER: " + listPlayersWinner.get(0).getName() + " ");
            sleep(gameSpeed); // Pause for emphasis.
            // Display the winning hand.
            for(Card tempCard: listPlayersWinner.get(0).getHandCards()) {
                System.out.print(tempCard.toString() + " ");
            }
            sleep(gameSpeed); // Pause for readability.
            // Announce the winner's new bank balance after receiving the pot.
            System.out.println("\nNew Bank: $" + listPlayersWinner.get(0).getBank() + " (+$" + tablePot + ")");
            sleep(gameSpeed); // Pause before continuing.
            System.out.println();
        } else {
            // In case of multiple winners, split the pot evenly among them.
            int splitPotAmount = tablePot / listPlayersWinner.size();
            for (Player winner : listPlayersWinner) {
                // Adjust each winner's bank balance with their share of the pot.
                winner.adjustPlayerBank(splitPotAmount);
                // Announce each winner and their new bank balance.
                System.out.println("SPLIT POT: " + winner.getName());
                System.out.print("New Bank: $" + winner.getBank() + " (+$" + splitPotAmount + ") ");
                // Display the winning hand for the first winner as an example (could be adjusted to show all if needed).
                for(Card tempCard: listPlayersWinner.get(0).getHandCards()) {
                    System.out.print(tempCard.toString() + " ");
                }
                System.out.println();
                sleep(gameSpeed); // Pause for readability between winners.
            }
        }
    }

    private void dealTableCards(int num) {
        // Deal 'num' community cards to the table from the deck.
        for(int i = 0; i < num; i++) {
            tableCards.add(deck.topCard());
        }
    }

    private void dealHoleCards(int num) {
        // Deal 'num' hole cards to each player from the deck.
        for(int i = 0; i < num; i++) {
            for(Player tempPlayer: listPlayersRemainingGame) {
                tempPlayer.addCardToPlayerHand(deck.topCard());
            }
        }
    }

    private void rotateDealer() {
        // Set the dealer to the first player in the list.
        dealer = listPlayersRemainingGame.get(0);
        // Assign small and big blind positions based on the number of players.
        if(listPlayersRemainingGame.size() <= 2) {
            // In heads-up play, the dealer is also the small blind.
            small = listPlayersRemainingGame.get(0);
            big = listPlayersRemainingGame.get(1);
        } else {
            // In games with more than two players, the blinds follow the dealer.
            small = listPlayersRemainingGame.get(1);
            big = listPlayersRemainingGame.get(2);
        }
        // Collect ante from the small and big blinds.
        ante();
    }

    private void ante() {
        // Adjust ante amounts based on player bank balances and update the pot.
        int smallAdjustedAnte = 0;
        int bigAdjustedAnte = 0;
        // Collect ante from the small blind, adjusting if their bank is less than the required ante.
        if(small.getBank() < tableAnteSmall) {
            smallAdjustedAnte = small.getBank();
            small.adjustPlayerBank(-smallAdjustedAnte);;
            tablePot += smallAdjustedAnte;
        } else {
            small.adjustPlayerBank(-tableAnteSmall);
            tablePot += tableAnteSmall;
        }
        // Collect ante from the big blind, adjusting if their bank is less than the required ante.
        if(big.getBank() < tableAnteBig) {
            bigAdjustedAnte = big.getBank();
            // This line seems to mistakenly adjust the small player's bank instead of the big player's.
            // It should be `big.adjustPlayerBank(-bigAdjustedAnte);`
            small.adjustPlayerBank(-bigAdjustedAnte);;
            tablePot += bigAdjustedAnte;
        } else {
            big.adjustPlayerBank(-tableAnteBig);
            tablePot += tableAnteBig;
        }

        // Decrease the ante countdown and double the ante amounts when it reaches zero.
        tableAnteCountdown--;
        if(tableAnteCountdown == 0) {
            tableAnteSmall += tableAnteSmall;
            tableAnteBig = tableAnteSmall*2;
            tableMinBet = tableAnteBig;
            // Reset the ante countdown based on the number of players in the round.
            tableAnteCountdown = listPlayersRemainingRound.size();
        }
    }

    private void printWelcome() {
        // Print a welcome message and house rules at the start of the game.
        System.out.println("Welcome to the Poker Party");
        sleep(gameSpeed);
        System.out.println("House Rules:");
        sleep(gameSpeed);
        System.out.println("Dealer rotates, but first action is on the dealer");
        sleep(gameSpeed);
        System.out.println("Ante increases after full dealer rotation");
        sleep(gameSpeed);
        System.out.println("Minimum bet is the big blind");
        sleep(gameSpeed);
        System.out.println();
    }

    private void printGameDetails() {
        // Print a decorative line to separate game details visually.
        for(int i = 0; i < 20; i++) {
            System.out.print("~");
        }
        System.out.println();

        // Display the game stage based on the current round stage.
        if(numRoundStage == 0) {
            // Pre-flop stage: Display game number and the roles of dealer, small blind, and big blind along with their ante amounts.
            System.out.println("GAME #" + numTotalGames);
            sleep(gameSpeed); // Pause for readability.
            System.out.println("PRE-FLOP");
            sleep(gameSpeed);
            System.out.println("Dealer: " + dealer.getName());
            sleep(gameSpeed);
            System.out.println("Small: " + small.getName() + ", $" + tableAnteSmall);
            sleep(gameSpeed);
            System.out.println("Big: " + big.getName() + ", $" + tableAnteBig);
            sleep(gameSpeed);
            System.out.println();
        }
        else if(numRoundStage == 1) {
            // Flop stage: Display "FLOP" to indicate the stage.
            System.out.println("FLOP");
            sleep(gameSpeed);
        }
        else if(numRoundStage == 2) {
            // Turn stage: Display "TURN" to indicate the stage.
            System.out.println("TURN");
            sleep(gameSpeed);
        }
        else if(numRoundStage == 3) {
            // River stage: Display "RIVER" to indicate the stage.
            System.out.println("RIVER");
            sleep(gameSpeed);
        }

        // Display the community cards on the table.
        System.out.println("Table Cards:");
        if(tableCards.size() == 0) {
            // If no cards are on the table, display placeholders.
            System.out.print("[  ] [  ]");
            sleep(gameSpeed);
        }
        for(Card tempCard: tableCards) {
            // Print each community card.
            System.out.print(tempCard.toString() + " ");
        }
        sleep(gameSpeed); // Pause for readability.
        System.out.println("\n");

        // Display details for each player still in the round and not folded.
        for(Player tempPlayer: listPlayersRemainingRound) {
            if(!tempPlayer.isFold()) {
                System.out.println("Player: " + tempPlayer.getName() + ", Bank: $" + tempPlayer.getBank());
                // Print each player's hand cards.
                for(Card tempCard: tempPlayer.getHandCards()) {
                    System.out.print(tempCard.toString() + " ");
                }
                // Print the evaluation of the player's hand.
                System.out.println(tempPlayer.evaluatePlayerHand());
                sleep(gameSpeed); // Pause for readability.
                System.out.println();
            }
        }
        System.out.println();
    }

    // Sets the current table bet to the specified amount.
    void setTableBet(int tableBet) {
        this.tableBet = tableBet;
    }

    // Adds a specified number of temporary players to the game.
    private void addTempPlayers(int numPlayers) {
        for(int i = 0; i < numPlayers; i++) {
            // Generate a unique name for each temporary player.
            String tempName = "Template#" + i;
            // Add the temporary player to the list of players still in the game.
            listPlayersRemainingGame.add(new TemplatePlayer(tempName));
        }
    }

    // Adds a specified number of simple NPC players to the game.
    private void addSimpleNPCs(int numPlayers) {
        for(int i = 0; i < numPlayers; i++) {
            // Generate a unique name for each simple NPC player.
            String tempName = "Simple#" + i;
            // Add the simple NPC player to the list of players still in the game.
            listPlayersRemainingGame.add(new SimpleNPCPlayer(tempName));
        }
    }

    // Adds a specified number of random NPC players to the game.
    private void addRandomNPCs(int numPlayers) {
        for(int i = 0; i < numPlayers; i++) {
            // Generate a unique name for each random NPC player.
            String tempName = "Random#" + i;
            // Add the random NPC player to the list of players still in the game.
            listPlayersRemainingGame.add(new RandomPlayer(tempName));
        }
    }

    // Adds a specified number of random NPC players to the game.
    private void addConservativeNPCs(int numPlayers) {
        for(int i = 0; i < numPlayers; i++) {
            // Generate a unique name for each random NPC player.
            String tempName = "Conversative#" + i;
            // Add the random NPC player to the list of players still in the game.
            listPlayersRemainingGame.add(new ConservativeNPCPlayer(tempName));
        }
    }

    // Pauses the game for a specified number of milliseconds.
    private void sleep(int ms) {
        try {
            // Attempt to pause the thread for the given duration.
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            // If the thread is interrupted, mark it as such and print an error message.
            Thread.currentThread().interrupt();
            System.out.println("Thread was interrupted, Failed to complete operation");
        }
    }
}


