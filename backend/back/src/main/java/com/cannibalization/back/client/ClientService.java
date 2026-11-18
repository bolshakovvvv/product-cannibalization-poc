package com.cannibalization.back.client;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientSnapshotRepository repository;

    @Transactional(readOnly = true)
    public ClientCardResponse getCard(String clientId) {
        ClientSnapshotEntity snapshot = repository
                .findFirstByClientIdOrderByAsOfAtDesc(clientId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Данные клиента не найдены"
                ));

        return new ClientCardResponse(
                snapshot.getClientId(),
                snapshot.getId(),
                snapshot.getAsOfAt(),

                new ClientCardResponse.Profile(
                        snapshot.getAge(),
                        snapshot.getGender(),
                        snapshot.getRegion(),
                        snapshot.getSegment(),
                        snapshot.getPayrollClient(),
                        snapshot.getBankTenureMonths(),
                        snapshot.getFamilyOrChildren()
                ),

                new ClientCardResponse.Insurance(
                        snapshot.getActiveDms(),
                        snapshot.getActiveAccidentPolicy(),
                        snapshot.getActivePolicyCount(),
                        snapshot.getHadInsurancePolicy(),
                        snapshot.getDaysSinceDmsPurchase()
                ),

                new ClientCardResponse.Communications(
                        snapshot.getOncoPushes30d(),
                        snapshot.getAllProductPushes7d(),
                        snapshot.getAllProductPushes30d(),
                        snapshot.getDaysSinceOncoPush(),
                        snapshot.getDaysSinceDmsPush(),
                        snapshot.getOpenRate180d(),
                        snapshot.getCtr180d(),
                        snapshot.getPreviousPushProduct()
                ),

                new ClientCardResponse.Spending(
                        snapshot.getMedicalSpend90d(),
                        snapshot.getPharmacySpend90d(),
                        snapshot.getAvgMonthlySpend()
                )
        );
    }
}