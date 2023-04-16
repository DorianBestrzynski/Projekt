package com.zpi.tripgroupservice.trip_group;

import com.zpi.tripgroupservice.commons.Currency;
import com.zpi.tripgroupservice.commons.GroupStage;
import com.zpi.tripgroupservice.invitation.Invitation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.zpi.tripgroupservice.commons.Utils.DEFAULT_DESCRIPTION;
import static com.zpi.tripgroupservice.commons.Utils.DEFAULT_VOTES_LIMIT;

@Entity
@Getter
@NoArgsConstructor
@Setter
@EqualsAndHashCode
public class TripGroup {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "group_sequence"
    )
    @SequenceGenerator(
            name = "group_sequence",
            sequenceName = "group_sequence", allocationSize = 1)
    @Column(unique = true, nullable = false)
    private Long groupId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "currency", nullable = false)
    @Enumerated(EnumType.STRING)
    private Currency currency;

    @Column(name = "description", length = 120)
    private String description;

    @Column(name = "destination_location", nullable = false, length = 100)
    private String destinationLocation;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "group_stage", nullable = false)
    @Enumerated(EnumType.STRING)
    private GroupStage groupStage;

    @OneToMany(mappedBy = "tripGroup", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Set<Invitation> invitations = new HashSet<>();


    public TripGroup(String name, Currency currency, String description, String destinationLocation) {
        this.name = name;
        this.currency = currency;
        this.description = Objects.requireNonNullElse(description,DEFAULT_DESCRIPTION + name);
        this.destinationLocation = destinationLocation;
        this.groupStage = GroupStage.TRIP_STAGE;
    }

    public TripGroup(String name, Currency currency, String description) {
        this.name = name;
        this.currency = currency;
        this.description = Objects.requireNonNullElse(description,DEFAULT_DESCRIPTION + name);
    }

    public void addInvitation(Invitation invitation) {
        invitations.add(invitation);
    }


}

