package com.gamematcher.dto.pubg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PUBG roster relationships (participants, team)
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PubgRosterRelationshipsDto {

    private PubgRelationshipDataDto participants;
    private PubgRelationshipDataDto team;
}
