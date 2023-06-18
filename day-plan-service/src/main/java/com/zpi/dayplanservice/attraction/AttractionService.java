package com.zpi.dayplanservice.attraction;

import com.google.maps.DistanceMatrixApi;
import com.google.maps.GeoApiContext;
import com.google.maps.PlaceDetailsRequest;
import com.google.maps.PlacesApi;
import com.google.maps.model.*;
import com.zpi.dayplanservice.aspects.AuthorizeCoordinator;
import com.zpi.dayplanservice.aspects.AuthorizePartOfTheGroup;
import com.zpi.dayplanservice.day_plan.DayPlanService;
import com.zpi.dayplanservice.dto.*;
import com.zpi.dayplanservice.exception.ApiRequestException;
import com.zpi.dayplanservice.mapstruct.MapStructMapper;
import com.zpi.dayplanservice.proxies.TripGroupProxy;
import com.zpi.dayplanservice.security.CustomUsernamePasswordAuthenticationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.zpi.dayplanservice.exception.ExceptionInfo.ATTRACTION_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttractionService {
    private final AttractionRepository attractionRepository;

    private final DayPlanService dayPlanService;

    private final GeoApiContext context;

    private final MapStructMapper mapstructMapper;

    private final TripGroupProxy tripGroupProxy;

    private static final int RADIUS_DISTANCE = 50000;

    @AuthorizePartOfTheGroup
    public List<Attraction> getAllAttractionsForDay(Long groupId, Long dayPlanId) {
        return dayPlanService.getDayPlanById(dayPlanId).getDayAttractions().stream().toList();
    }

    public List<AttractionCandidateDto> findCandidates(String name) {
        var foundCandidates = PlacesApi.textSearchQuery(context, name)
                                       .awaitIgnoreError();

        var result = convertToAttractionCandidateDto(foundCandidates.results);
        getUrl(result);
        System.out.println(foundCandidates);
        return result;
    }

    public List<AttractionCandidateDto> findNearbyCandidates(Double longitude, Double latitude, String  queryCategory, RankByType rankByType) {
        var coordinates = new LatLng(latitude, longitude);
        var rankBy = parseToRankBy(rankByType);

        var foundCandidates = Optional.ofNullable(queryCategory)
                .map(category -> findNearbyCandidatesByLocationAndCategory(coordinates, category, rankBy))
                .orElseGet(() -> findNearbyCandidatesByLocation(coordinates));

        var result = convertToAttractionCandidateDto(foundCandidates.results);
        getUrl(result);
        return result;
    }


    private PlacesSearchResponse findNearbyCandidatesByLocation(LatLng coordinates) {
            log.info("Coordinates " + coordinates.lat + coordinates.lng);
            var request = PlacesApi.nearbySearchQuery(context, coordinates);
            request.radius(RADIUS_DISTANCE);

            return request.awaitIgnoreError();
    }

    private PlacesSearchResponse findNearbyCandidatesByLocationAndCategory(LatLng coordinates, String category, RankBy rankBy) {
        try {
            var placeType = PlaceType.valueOf(category.toUpperCase());
            log.info("Performing search by location and category");
            log.info("Place type is: " + placeType);
            log.info("RankBy type is: " + rankBy);
            var request =  PlacesApi.nearbySearchQuery(context, coordinates);
            if (rankBy.equals(RankBy.PROMINENCE)) {
                request.radius(RADIUS_DISTANCE);
            }
            else {
                request.rankby(rankBy);
            }
            request.type(placeType);

            return request.awaitIgnoreError();

        } catch (Exception ex) {
            log.info("Wrong place type, performing textSearch query");
            var request = PlacesApi.textSearchQuery(context, category, coordinates);
            request.radius(RADIUS_DISTANCE);
            return request.awaitIgnoreError();
        }
    }

    private RankBy parseToRankBy(RankByType rankByType) {
        if (rankByType == null) {
            return RankBy.PROMINENCE;
        }
        return rankByType == RankByType.DISTANCE ? RankBy.DISTANCE : RankBy.PROMINENCE;
    }

    @Transactional
    @AuthorizeCoordinator
    public Attraction deleteAttraction(Long attractionId, Long dayPlanId) {
        if(attractionId == null || dayPlanId == null || attractionId < 0 || dayPlanId < 0)
            throw new IllegalArgumentException("Attraction id or day plan id is null");

        var dayPlan = dayPlanService.getDayPlanById(dayPlanId);

        var toDelete = dayPlan.deleteAttraction(attractionId);
        toDelete.removeDay(dayPlan);

        if(toDelete.getDays().isEmpty())
            attractionRepository.delete(toDelete);

        return toDelete;
    }

    @Transactional
    public Attraction addAttraction(List<Long> dayPlanIds, AttractionCandidateDto attractionCandidateDto) {
        CustomUsernamePasswordAuthenticationToken authentication = (CustomUsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        var userId = authentication.getUserId();
        if(dayPlanIds.isEmpty())
            throw new IllegalArgumentException("Day plan ids cannot be empty");

        if(userId == null)
            throw new IllegalArgumentException("User id cannot be null");

        var days = dayPlanService.getDayPlanById(dayPlanIds, userId);
        if(days.size() != dayPlanIds.size())
            throw new IllegalArgumentException("Day plan not found");

        var attraction = mapstructMapper.getAttractionFromCandidateDto(attractionCandidateDto);
        attraction.addDays(days);

        for (var day : days) {
            day.addAttraction(attraction);
        }

        return attractionRepository.save(attraction);
    }

    private List<AttractionCandidateDto> convertToAttractionCandidateDto(PlacesSearchResult[] foundCandidates) {
        if(foundCandidates == null)
            return new ArrayList<>();

        var result = new ArrayList<AttractionCandidateDto>();
        for (PlacesSearchResult foundCandidate : foundCandidates) {
            var candidate = new AttractionCandidateDto(foundCandidate.name,
                                                       foundCandidate.geometry.location.lat,
                                                       foundCandidate.geometry.location.lng,
                                                       foundCandidate.placeId,
                                                       foundCandidate.photos == null ? null : foundCandidate.photos[0].photoReference,
                                                       foundCandidate.formattedAddress);
            result.add(candidate);
        }
        return result;
    }

    private List<AttractionCandidateDto> getUrl(List<AttractionCandidateDto> candidates) {
        if(candidates == null)
            return new ArrayList<>();

        for (AttractionCandidateDto candidate : candidates) {
            var placeDetails = PlacesApi.placeDetails(context, candidate.getPlaceId())
                                        .fields(PlaceDetailsRequest.FieldMask.URL,
                                                PlaceDetailsRequest.FieldMask.OPENING_HOURS,
                                                PlaceDetailsRequest.FieldMask.FORMATTED_ADDRESS)
                                        .awaitIgnoreError();
            candidate.setUrl(placeDetails.url.toString());
            candidate.setOpeningHours(new String[] {});
            candidate.setAddress(placeDetails.formattedAddress);

        }
        return candidates;
    }

    @Transactional
    public Attraction editAttraction(Attraction attraction) {
        if(attraction == null)
            throw new IllegalArgumentException("User id or attraction candidate dto is null");

        var currentAttraction = attractionRepository.findById(attraction.getAttractionId()).orElseThrow(() -> new ApiRequestException(ATTRACTION_NOT_FOUND));

        if(attraction.getDescription() == null){
            return currentAttraction;
        }
        currentAttraction.setDescription(attraction.getDescription());
        return attractionRepository.save(currentAttraction);
    }

    public List<AttractionPlanDto> findOptimalDayPlan(Long dayPlanId) {
        var dayPlan = dayPlanService.getDayPlanById(dayPlanId);
        var attractions = new ArrayList<>(dayPlan.getDayAttractions());

        if(dayPlan.getDayPlanStartingPointId() == null) {
            var accommodation = tripGroupProxy.getAccommodation("internalCommunication",dayPlan.getGroupId());
            if(accommodation == null || accommodation.destinationLatitude() == null || accommodation.destinationLongitude() == null)
                return findBestAttractionsOrder(attractions).attractions();

            attractions.add(new Attraction(accommodation.destinationLatitude(), accommodation.destinationLongitude()));
            var result = findBestAttractionsOrder(attractions, attractions.size() - 1).attractions();
            result.remove(0);
            return result;
        }
        else return findBestAttractionsOrder(attractions, IntStream.range(0, attractions.size())
                                                           .filter(i -> attractions.get(i).getAttractionId().equals(dayPlan.getDayPlanStartingPointId()))
                                                           .findAny()
                                                           .orElse(0)).attractions();

    }

    public RouteDto findBestAttractionsOrder(List<Attraction> attractions) {
        long minDistance = Long.MAX_VALUE;
        RouteDto minRoute = null;
        for (int i = 0; i < attractions.size(); i++) {
            var route = findBestAttractionsOrder(attractions, i);
            if (route.distance() < minDistance) {
                minDistance = route.distance();
                minRoute = route;
            }
        }
        return minRoute;
    }

    public RouteDto findBestAttractionsOrder(List<Attraction> attractions, int startingPointIndex) {
        var distanceMatrix = getDistanceMatrix(attractions);
        var attractionList = new LinkedHashSet<Attraction>();
        var attractionDtoList = new ArrayList<AttractionPlanDto>();

        int currentAttractionIndex = startingPointIndex;
        attractionList.add(attractions.get(currentAttractionIndex));
        attractionDtoList.add(new AttractionPlanDto(attractions.get(currentAttractionIndex)));

        var routeDistance = 0L;

        while(attractions.size() != attractionList.size()) {
            var minDistance = Long.MAX_VALUE;
            var rowElems = distanceMatrix.rows[currentAttractionIndex].elements;
            int bestAttractionIndex = -1;

            for(int i = 0; i < rowElems.length; i++) {
                if(i == currentAttractionIndex)
                    continue;

                if(rowElems[i].distance.inMeters < minDistance && !attractionList.contains(attractions.get(i))) {
                    bestAttractionIndex = i;
                    minDistance = rowElems[i].distance.inMeters;
                }
            }

            if(minDistance != Long.MAX_VALUE) {
                routeDistance += minDistance;
                attractionDtoList.get(attractionDtoList.size() - 1)
                                 .setDistanceToNextAttraction(Long.valueOf(minDistance)
                                                                  .doubleValue());
            }


            attractionList.add(attractions.get(bestAttractionIndex));
            attractionDtoList.add(new AttractionPlanDto(attractions.get(bestAttractionIndex)));
            currentAttractionIndex = bestAttractionIndex;
        }

        return new RouteDto(new ArrayList<>(attractionDtoList), routeDistance);
    }


    private DistanceMatrix getDistanceMatrix(List<Attraction> attractions) {
        var attractionsCoordinates = getCoordinates(attractions);
        var attractionsCoordinatesArray = attractionsCoordinates.toArray(new LatLng[0]);
        return DistanceMatrixApi.newRequest(context)
                                              .origins(attractionsCoordinatesArray)
                                              .destinations(attractionsCoordinatesArray)
                                              .awaitIgnoreError();
    }

    private List<LatLng> getCoordinates(List<Attraction> attractions) {
        return attractions.stream()
                          .map(attraction -> new LatLng(attraction.getLatitude(), attraction.getLongitude()))
                          .collect(Collectors.toList());
    }
}
