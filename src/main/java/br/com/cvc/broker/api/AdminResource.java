package br.com.cvc.broker.api;

import java.time.LocalDate;

import br.com.cvc.broker.api.dto.AdminRequests.AvailabilityUpsertBatch;
import br.com.cvc.broker.api.dto.AdminRequests.HotelUpsert;
import br.com.cvc.broker.api.dto.AdminRequests.PlanUpsert;
import br.com.cvc.broker.api.dto.AdminRequests.RateUpsertBatch;
import br.com.cvc.broker.api.dto.AdminRequests.RoomUpsert;
import br.com.cvc.broker.api.dto.HotelDetail;
import br.com.cvc.broker.catalogue.AvailabilityRepository;
import br.com.cvc.broker.catalogue.CatalogueRepository;
import br.com.cvc.broker.catalogue.IngestionRepository;
import br.com.cvc.broker.catalogue.RateRepository;
import br.com.cvc.broker.exceptions.BrokerException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Ingestao: o operador publica catalogo, tarifas e disponibilidade.
 *
 * <p>Tudo aqui e idempotente por chave natural (hotel, quarto+plano+data) e renova
 * {@code updated_at}, que e o que faz o {@code staleAfter} da cotacao andar.</p>
 */
@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminResource {

    private final CatalogueRepository catalogue;
    private final RateRepository rates;
    private final AvailabilityRepository availability;
    private final IngestionRepository ingestion;

    public AdminResource(final CatalogueRepository catalogue, final RateRepository rates,
                    final AvailabilityRepository availability, final IngestionRepository ingestion) {
        this.catalogue = catalogue;
        this.rates = rates;
        this.availability = availability;
        this.ingestion = ingestion;
    }

    @PUT
    @Path("/hotels/{hotelId}")
    @Transactional
    public HotelDetail upsertHotel(@PathParam("hotelId") final long hotelId, @Valid final HotelUpsert body) {
        this.ingestion.upsertHotel(hotelId, body.name(), body.cityCode(), body.cityName(), body.latitude(),
                        body.longitude());

        for (final RoomUpsert roomBody : body.roomTypes()) {
            final Long owner = this.ingestion.findRoomHotelId(roomBody.id());
            if (owner != null && owner != hotelId) {
                throw new BrokerException("room-not-found", 404,
                                "Room " + roomBody.id() + " does not belong to hotel " + hotelId);
            }
            this.ingestion.upsertRoom(roomBody.id(), hotelId, roomBody.name(), roomBody.maxAdults(),
                            roomBody.maxChildren());

            for (final PlanUpsert planBody : roomBody.ratePlans()) {
                final Long roomId = this.ingestion.findPlanRoomId(planBody.id());
                if (roomId != null && roomId != roomBody.id()) {
                    throw new BrokerException("rate-plan-not-found", 404,
                                    "Rate plan " + planBody.id() + " does not belong to room " + roomBody.id());
                }
                this.ingestion.upsertPlan(planBody.id(), roomBody.id(), planBody.name(), planBody.board(),
                                planBody.cancellationPolicy());
            }
        }

        return new HotelDetail(hotelId, body.name(), body.cityCode(), body.cityName(), body.latitude(),
                        body.longitude(), "PUSH", this.ingestion.now(),
                        body.roomTypes().stream()
                                        .map(room -> new HotelDetail.Room(room.id(), room.name(), room.maxAdults(),
                                                        room.maxChildren(),
                                                        room.ratePlans().stream()
                                                                        .map(plan -> new HotelDetail.Plan(plan.id(),
                                                                                        plan.name(), plan.board(),
                                                                                        plan.cancellationPolicy()))
                                                                        .toList()))
                                        .toList());
    }

    @PUT
    @Path("/rooms/{roomTypeId}/rates")
    @Transactional
    public Response publishRates(@PathParam("roomTypeId") final long roomTypeId,
                    @Valid final RateUpsertBatch body) {
        requireRoom(roomTypeId);
        requirePlanOfRoom(body.ratePlanId(), roomTypeId);
        requireRange(body.from(), body.to());

        this.rates.publish(roomTypeId, body.ratePlanId(), body.from(), body.to(), body.adultAmount(),
                        body.childAmount(), body.currency());

        return Response.noContent().build();
    }

    @PUT
    @Path("/rooms/{roomTypeId}/availability")
    @Transactional
    public Response publishAvailability(@PathParam("roomTypeId") final long roomTypeId,
                    @Valid final AvailabilityUpsertBatch body) {
        requireRoom(roomTypeId);
        requireRange(body.from(), body.to());

        this.availability.publish(roomTypeId, body.from(), body.to(), body.allotment(), body.stopSell());

        return Response.noContent().build();
    }

    private void requireRoom(final long roomTypeId) {
        this.catalogue.room(roomTypeId)
                        .orElseThrow(() -> new BrokerException("room-not-found", 404,
                                        "No room with id " + roomTypeId));
    }

    private void requirePlanOfRoom(final long ratePlanId, final long roomTypeId) {
        final var plan = this.catalogue.plan(ratePlanId)
                        .orElseThrow(() -> new BrokerException("rate-plan-not-found", 404,
                                        "No rate plan with id " + ratePlanId));
        if (!plan.roomTypeId.equals(roomTypeId)) {
            throw new BrokerException("rate-plan-not-found", 404,
                            "Rate plan " + ratePlanId + " does not belong to room " + roomTypeId);
        }
    }

    private static void requireRange(final LocalDate from, final LocalDate to) {
        if (to.isBefore(from)) {
            throw new BrokerException("invalid-period", 400, "`to` must not be before `from`");
        }
    }
}
