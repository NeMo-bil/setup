Feature: Buchungsanfrage

  Scenario: Buchung eines Cabs.
    Given alle Services laufen
    And Alle Subscriptions sind angelegt
    And Cabs existieren im System
    And Kunde hat valide Zahlungsinformationen
    When der Nutzer eine Fahrt für 1 Person und 1 Cab bucht
    Then soll die operative Planung 3 Vorschlaege schicken
    And der Nutzer einen auswaehlen
    And der Bezahlservice die Buchung erlaubt
    And die Operative Planung die Buchung bestaetigt