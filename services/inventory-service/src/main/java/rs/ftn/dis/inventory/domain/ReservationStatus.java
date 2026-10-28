package rs.ftn.dis.inventory.domain;

public enum ReservationStatus {
    /** roba je obecana porudzbini, placanje jos nije zavrseno */
    ACTIVE,
    /** porudzbina je otkazana, roba je vracena u dostupnu */
    RELEASED,
    /** porudzbina je placena, roba je trajno skinuta sa stanja */
    COMMITTED
}
