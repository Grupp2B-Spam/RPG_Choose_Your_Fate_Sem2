package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.*;

@Entity
@Table(name = "npc")
public class Npc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 45)
    private String name;

    @ManyToOne(optional = false)
    @JoinColumn(name = "race_details_id", nullable = false)
    private RaceDetails raceDetails;

    public Npc() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public RaceDetails getRaceDetails() { return raceDetails; }
    public void setRaceDetails(RaceDetails raceDetails) { this.raceDetails = raceDetails; }
}
