package com.zpi.tripgroupservice.user_group;

import com.zpi.tripgroupservice.commons.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class UserGroup {

    @EmbeddedId
    @Getter
    private UserGroupKey id;

    @Getter
    @Setter
    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

}





