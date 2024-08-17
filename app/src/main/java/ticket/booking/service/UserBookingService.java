package ticket.booking.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ticket.booking.entities.Ticket;
import ticket.booking.entities.Train;
import ticket.booking.entities.User;
import ticket.booking.util.UserServiceUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UserBookingService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<User> userList;
    private final String USER_FILE_PATH = "app/src/main/java/ticket/booking/localDB/users.json";

    public UserBookingService() throws IOException {
        loadUserListFromFile();
    }

    private void loadUserListFromFile() throws IOException {
        userList = objectMapper.readValue(new File(USER_FILE_PATH), new TypeReference<List<User>>() {});
    }

    private void saveUserListToFile() throws IOException {
        objectMapper.writeValue(new File(USER_FILE_PATH), userList);
    }

    public User loginUser(String name, String password) {
        Optional<User> foundUser = userList.stream()
                .filter(user -> user.getName().equals(name) && UserServiceUtil.checkPassword(password, user.getHashedPassword()))
                .findFirst();
        return foundUser.orElse(null);
    }

    public boolean signUp(User user) {
        if (userList.stream().anyMatch(u -> u.getName().equals(user.getName()))) {
            System.out.println("User already exists!");
            return false;
        }
        userList.add(user);
        try {
            saveUserListToFile();
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    public boolean fetchBookings(User user) {
        if (user.getTicketsBooked().isEmpty()) {
            return false;  // No bookings found
        } else {
            System.out.println("\n===== Your Bookings =====");
            for (Ticket ticket : user.getTicketsBooked()) {
                System.out.println("==================================");
                System.out.println("| Ticket ID  : " + ticket.getTicketId());
                System.out.println("| Train ID   : " + ticket.getTrain().getTrainId());
                System.out.println("| From       : " + ticket.getSource());
                System.out.println("| To         : " + ticket.getDestination());
                System.out.println("| Date       : " + ticket.getDateOfTravel());
                System.out.println("| Train No   : " + ticket.getTrain().getTrainNo());
                System.out.println("==================================\n");
            }
            return true;
        }
    }

    public boolean cancelBooking(User user, String ticketId) {
        Optional<Ticket> ticketToCancel = user.getTicketsBooked().stream()
                .filter(ticket -> ticket.getTicketId().equals(ticketId))
                .findFirst();
        if (ticketToCancel.isPresent()) {
            user.getTicketsBooked().remove(ticketToCancel.get());
            try {
                saveUserListToFile();
                return true;
            } catch (IOException ex) {
                return false;
            }
        } else {
            System.out.println("No ticket found with ID: " + ticketId);
            return false;
        }
    }

    public List<Train> getTrains(String source, String destination) {
        try {
            TrainService trainService = new TrainService();
            return trainService.searchTrains(source, destination);
        } catch (IOException ex) {
            return new ArrayList<>();
        }
    }

    public List<List<Integer>> fetchSeats(Train train) {
        return train.getSeats();
    }

    public Boolean bookTrainSeat(Train train, int row, int seat, User user) {
        try {
            TrainService trainService = new TrainService();
            List<List<Integer>> seats = train.getSeats();
            if (row >= 0 && row < seats.size() && seat >= 0 && seat < seats.get(row).size()) {
                if (seats.get(row).get(seat) == 0) {
                    seats.get(row).set(seat, 1);  // Mark the seat as booked
                    train.setSeats(seats);
                    trainService.updateTrain(train);  // Save the updated train details

                    // Create a new ticket for the booking
                    Ticket ticket = new Ticket(
                            UUID.randomUUID().toString(),
                            user.getUserId(),
                            train.getStations().get(0),  // Assuming source is the first station
                            train.getStations().get(train.getStations().size() - 1),  // Assuming destination is the last station
                            "2024-09-01T18:30:00Z",  // Just an example date
                            train
                    );

                    user.getTicketsBooked().add(ticket);
                    saveUserListToFile();  // Save the user's booking details

                    return true;  // Booking successful
                } else {
                    return false;  // Seat is already booked
                }
            } else {
                return false;  // Invalid row or seat index
            }
        } catch (IOException ex) {
            return false;
        }
    }
}
