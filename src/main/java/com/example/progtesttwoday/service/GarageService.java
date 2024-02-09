package com.example.progtesttwoday.service;

import com.example.progtesttwoday.repository.ParkedShipRepository;
import com.example.progtesttwoday.responseObject.OutResponseDto;
import com.example.progtesttwoday.entity.Garage;
import com.example.progtesttwoday.entity.ParkedShip;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class GarageService {

    private final Garage garage;
    private final ParkedShipRepository parkedShipRepository;

    private static final int PARKING_COST = 15;
    private static final int MAX_HOURS_FOR_DAY_RATE = 24;
    private static final double DAY_RATE = 50.0;

    private double calculateParkingCost (int parkingLotNumber) {
        LocalDateTime timeNow = LocalDateTime.now();
        LocalDateTime parkingTime = garage.getParkingLots()[parkingLotNumber - 1].getParkingDateTime();
        Duration duration = Duration.between(parkingTime, timeNow);
        long hours = duration.toHours();
        double cost;

        if (hours >= 1 && hours <= MAX_HOURS_FOR_DAY_RATE) {
            cost = hours * PARKING_COST;
        } else if (hours < 1) {
            cost = PARKING_COST;
        } else {
            long days = hours / MAX_HOURS_FOR_DAY_RATE;
            long remainingHours = hours % MAX_HOURS_FOR_DAY_RATE;
            cost = days * DAY_RATE + remainingHours * PARKING_COST;
        }
        return cost;
    }

    private void populateGarageArray() {
        List<ParkedShip> parkedShips = parkedShipRepository.findByLeavingDateTimeIsNull();

        for (ParkedShip parkedShip : parkedShips) {
            garage.getParkingLots()[parkedShip.getParkingLot() - 1] = parkedShip;
        }
    }

    public ResponseEntity park(String licensePlate) {
        populateGarageArray();

        for (int i = 0; i < garage.getParkingLots().length; i++) {
            if (garage.getParkingLots()[i] == null) {
                ParkedShip parkedShip = new ParkedShip(licensePlate);
                garage.getParkingLots()[i] = parkedShip;
                parkedShip.setParkingLot(i + 1);
                parkedShipRepository.save(parkedShip);
                return ResponseEntity.ok().body("SHIP PARKED ON SPOT: " + (i + 1));
            }
        }
        return ResponseEntity.badRequest().body("No available spots");
    }

    public ResponseEntity leave(int parkingLotNumber) {

        if (parkingLotNumber < 1 || parkingLotNumber > garage.getParkingLots().length) {
            return ResponseEntity.badRequest().body("invalid parking lot number: " + parkingLotNumber);
        }

        ParkedShip parkedShip = garage.getParkingLots()[parkingLotNumber - 1];

        if (parkedShip == null) {
            return ResponseEntity.badRequest().body("No ship is parked on this parking lot: " + parkingLotNumber);
        }

        parkedShip.setLeavingDateTime(LocalDateTime.now());

        double cost = calculateParkingCost(parkingLotNumber);
        parkedShip.setFinalParkingCost(cost);

        garage.getParkingLots()[parkingLotNumber - 1] = null;

        parkedShipRepository.save(parkedShip);

        OutResponseDto outResponseDto = OutResponseDto.builder()
                .parkedShip(parkedShip)
                .cost(cost)
                .build();

        return ResponseEntity.ok().body(outResponseDto);
    }

    public ResponseEntity parkedShips() {
        return ResponseEntity.ok().body(parkedShipRepository.findAll());
    }

}