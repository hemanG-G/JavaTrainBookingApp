package ticket.booking;

import ticket.booking.entities.Train;
import ticket.booking.entities.User;
import ticket.booking.service.UserBookingService;
import ticket.booking.util.UserServiceUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

public class App {

    public static void main(String[] args) {
        System.out.println("Welcome to the Train Booking System!");
        Scanner scanner = new Scanner(System.in);
        int option = 0;
        UserBookingService userBookingService = null;
        User loggedInUser = null;
        List<Train> lastSearchedTrains = new ArrayList<>();  // To store the list of searched trains

        try {
            userBookingService = new UserBookingService();
        } catch (IOException ex) {
            System.out.println("Error loading data. Please try again.");
            return;
        }

        while (option != 7) {
            System.out.println("\n================================================");
            System.out.println(loggedInUser != null ? "Status: Logged in as " + loggedInUser.getName() : "Status: Not logged in");
            System.out.println("================================================");
            System.out.println("1. Sign up");
            System.out.println("2. Login");
            System.out.println("3. Fetch Bookings");
            System.out.println("4. Search Trains");
            System.out.println("5. Book a Seat");
            System.out.println("6. Cancel my Booking");
            System.out.println("7. Exit the App");
            System.out.println("================================================");
            System.out.print("Choose an option: ");
            option = scanner.nextInt();

            switch (option) {
                case 1:
                    System.out.println("Enter the username to sign up:");
                    String nameToSignUp = scanner.next();
                    System.out.println("Enter the password to sign up:");
                    String passwordToSignUp = scanner.next();
                    User userToSignUp = new User(nameToSignUp, passwordToSignUp,
                            UserServiceUtil.hashPassword(passwordToSignUp),
                            new ArrayList<>(), UUID.randomUUID().toString());
                    boolean signUpSuccess = userBookingService.signUp(userToSignUp);
                    System.out.println(signUpSuccess ? "Sign up successful!" : "Sign up failed!");
                    break;
                case 2:
                    System.out.println("Enter the username to login:");
                    String nameToLogin = scanner.next();
                    System.out.println("Enter the password to login:");
                    String passwordToLogin = scanner.next();
                    loggedInUser = userBookingService.loginUser(nameToLogin, passwordToLogin);
                    System.out.println(loggedInUser != null ? "Login successful!" : "Login failed!");
                    break;
                case 3:
                    if (loggedInUser != null) {
                        System.out.println("Fetching your bookings...");
                        boolean hasBookings = userBookingService.fetchBookings(loggedInUser);
                        if (!hasBookings) {
                            System.out.println("No bookings found.");
                        }
                    } else {
                        System.out.println("You need to login first!");
                    }
                    break;
                case 4:
                    if (loggedInUser != null) {
                        lastSearchedTrains = searchTrains(scanner, userBookingService);
                    } else {
                        System.out.println("You need to login first!");
                    }
                    break;
                case 5:
                    if (loggedInUser != null) {
                        if (lastSearchedTrains.isEmpty()) {
                            System.out.println("Let's first search for available trains.");
                            lastSearchedTrains = searchTrains(scanner, userBookingService);
                        }

                        if (!lastSearchedTrains.isEmpty()) {
                            bookSeat(scanner, userBookingService, loggedInUser, lastSearchedTrains);
                        }
                    } else {
                        System.out.println("You need to login first!");
                    }
                    break;
                case 6:
                    if (loggedInUser != null) {
                        System.out.println("Enter your ticket ID to cancel the booking:");
                        String ticketId = scanner.next();
                        boolean cancelSuccess = userBookingService.cancelBooking(loggedInUser, ticketId);
                        System.out.println(cancelSuccess ? "Booking cancelled successfully!" : "Failed to cancel booking.");
                    } else {
                        System.out.println("You need to login first!");
                    }
                    break;
                case 7:
                    System.out.println("Exiting the app... Thank you for using the Train Booking System!");
                    break;
                default:
                    System.out.println("Invalid option! Please try again.");
            }
        }
    }

    private static List<Train> searchTrains(Scanner scanner, UserBookingService userBookingService) {
        System.out.println("Type your source station:");
        String source = scanner.next();
        System.out.println("Type your destination station:");
        String dest = scanner.next();
        List<Train> trains = userBookingService.getTrains(source, dest);
        if (trains.isEmpty()) {
            System.out.println("No trains found for the given source and destination.");
            return new ArrayList<>();  // Return to menu if no trains found
        } else {
            int index = 1;
            System.out.println("\nAvailable Trains:");
            System.out.println("==================");
            for (Train t : trains) {
                System.out.println(index + ". Train ID: " + t.getTrainId());
                for (Map.Entry<String, String> entry : t.getStationTimes().entrySet()) {
                    System.out.println("   Station: " + entry.getKey() + " Time: " + entry.getValue());
                }
                index++;
            }
            System.out.println("==================");
            return trains;  // Return the list of searched trains
        }
    }

    private static void bookSeat(Scanner scanner, UserBookingService userBookingService, User loggedInUser, List<Train> lastSearchedTrains) {
        System.out.println("Select a train by typing 1, 2, 3...");
        int trainIndex = scanner.nextInt() - 1;

        if (trainIndex < 0 || trainIndex >= lastSearchedTrains.size()) {
            System.out.println("Invalid train selection.");
            return;
        }

        Train selectedTrain = lastSearchedTrains.get(trainIndex);

        System.out.println("Select a seat out of these seats:");
        List<List<Integer>> seats = userBookingService.fetchSeats(selectedTrain);
        System.out.print("    ");
        for (int i = 1; i <= seats.get(0).size(); i++) {
            System.out.printf(" %d ", i);  // Column numbers
        }
        System.out.println();
        int rowNum = 1;
        for (List<Integer> row : seats) {
            System.out.printf("%2d ", rowNum);  // Row numbers
            for (Integer val : row) {
                System.out.print((val == 0 ? "[ ]" : "[X]") + " ");
            }
            System.out.println();
            rowNum++;
        }
        System.out.println("Select the seat by typing the row and column.");
        System.out.println("Enter the row:");
        int row = scanner.nextInt();
        System.out.println("Enter the column:");
        int col = scanner.nextInt();
        System.out.println("Booking your seat...");
        Boolean booked = userBookingService.bookTrainSeat(selectedTrain, row - 1, col - 1, loggedInUser);
        if (booked) {
            System.out.println("Booked! Enjoy your journey.");
        } else {
            System.out.println("Can't book this seat.");
        }
    }
}
