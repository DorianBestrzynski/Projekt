package com.zpi.tripgroupservice.trip_group;

import com.zpi.tripgroupservice.commons.Currency;
import com.zpi.tripgroupservice.commons.Role;
import com.zpi.tripgroupservice.dto.*;
import com.zpi.tripgroupservice.user_group.UserGroup;
import com.zpi.tripgroupservice.user_group.UserGroupKey;
import com.zpi.tripgroupservice.user_group.UserGroupRepository;
import com.zpi.tripgroupservice.user_group.UserGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RequestMapping("api/v1/trip-group")
@RestController
@RequiredArgsConstructor
public class TripGroupController {
    private final TripGroupService tripGroupService;
    private final TripGroupRepository tripGroupRepository;
    private final UserGroupRepository userGroupRepository;

    @GetMapping("/groups/{userId}")
    public ResponseEntity<List<TripExtendedDataDto>> getAllGroupsForUser(@PathVariable Long userId) {
        var result = tripGroupService.getAllGroupsForUser(userId);
        return ResponseEntity.ok(result);
    }
    @PostMapping("/group")
    public ResponseEntity<TripGroup> createGroup(@Valid @RequestBody TripGroupDto tripGroupDto) {
        var result = tripGroupService.createGroup(tripGroupDto);
        return new ResponseEntity<>(result, HttpStatus.CREATED);

    }
    @DeleteMapping("/group")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void deleteGroup(@RequestParam(name = "groupId") Long groupId) {
        tripGroupService.deleteGroup(groupId);
    }

    @PatchMapping("/group")
    public ResponseEntity<TripGroup> changeGroup(@RequestParam Long groupId, @RequestBody TripGroupDto tripGroupDto) {
        var result = tripGroupService.updateGroup(groupId, tripGroupDto);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
    @GetMapping("/data")
    public ResponseEntity<TripExtendedDataDto> getTripData(@RequestParam Long groupId){
        var tripData = tripGroupService.getTripData(groupId);
        return ResponseEntity.ok(tripData);
    }

    @PatchMapping("/currency")
    public ResponseEntity<TripGroup> setCurrency(@RequestParam Long groupId, @RequestParam Currency currency){
        var result = tripGroupService.setCurrencyInGroup(groupId, currency);
        return new ResponseEntity<>(result, HttpStatus.ACCEPTED);
    }

    @DeleteMapping("/user")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void leaveGroup(@RequestParam Long groupId) {
        tripGroupService.leaveGroup(groupId);
    }

    @DeleteMapping("/coordinator-user")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void deleteUserFromGroup(@RequestParam Long groupId, @RequestParam Long userId) {
        tripGroupService.deleteUserFromGroup(groupId, userId);
    }

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public void changeGroupStage(@RequestParam Long groupId) {
        tripGroupService.changeGroupStage(groupId);
    }

    @GetMapping("/accommodation")
    public ResponseEntity<GroupInformationInfoDto> getAccommodation(@RequestParam Long groupId){
        var accommodation = tripGroupService.getAccommodation(groupId);
        return ResponseEntity.ok(accommodation);
    }

    @GetMapping("/sampleData")
    public String creatingSampleData() {
        var tripGroup = new TripGroup("Test1", Currency.PLN, "Opis");
        var tripGroup1 = new TripGroup("Test2", Currency.PLN, "Opis2");
        var tripGroup2 = new TripGroup("Test3", Currency.USD, "Opis3");
        var tripGroup3 = new TripGroup("Test4", Currency.PLN, "Opis4");
        var tripGroup4 = new TripGroup("Test5", Currency.PLN, "Opis5");
        var tripGroup5 = new TripGroup("Finance Optimizer", Currency.PLN, "Grupa testujaca optymalizacje");
        tripGroupRepository.saveAll(List.of(tripGroup1, tripGroup2, tripGroup3, tripGroup4, tripGroup, tripGroup5));

        var userData1 = new UserGroup(new UserGroupKey(1L, tripGroup.getGroupId()), Role.COORDINATOR);
        var userData2 = new UserGroup(new UserGroupKey(1L, tripGroup1.getGroupId()), Role.COORDINATOR);
        var userData3 = new UserGroup(new UserGroupKey(1L, tripGroup2.getGroupId()), Role.COORDINATOR);
        var userData4 = new UserGroup(new UserGroupKey(1L, tripGroup3.getGroupId()), Role.COORDINATOR);
        var userData5 = new UserGroup(new UserGroupKey(2L, tripGroup4.getGroupId()), Role.COORDINATOR);
        var userData6 = new UserGroup(new UserGroupKey(2L, tripGroup1.getGroupId()), Role.PARTICIPANT);
        var userData7 = new UserGroup(new UserGroupKey(1L, tripGroup5.getGroupId()), Role.COORDINATOR);
        var userData8 = new UserGroup(new UserGroupKey(2L, tripGroup5.getGroupId()), Role.PARTICIPANT);
        var userData9 = new UserGroup(new UserGroupKey(3L, tripGroup5.getGroupId()), Role.PARTICIPANT);
        var userData10 = new UserGroup(new UserGroupKey(4L, tripGroup5.getGroupId()), Role.PARTICIPANT);


        userGroupRepository.saveAll(List.of(userData1, userData2, userData3, userData4, userData5, userData6, userData7, userData8, userData9, userData10));

        return "Created sample data";
    }
}
