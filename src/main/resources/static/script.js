$(function() {
            $("#datepicker").datepicker({
                dateFormat: "yy-mm-dd",
                changeMonth: true,
                changeYear: true
            });

            // Funktion wenn Excel Datei ausgewählt wurde
            $("#excelUpload").change(function() {
                var file = this.files[0];                       // Datei wird der Variable "file" übergeben
                if (file && file.name.endsWith(".xlsx")) {      // Prüfung ob Datei leer und die Endung .xlsx hat
                    $("#excelUpload").prop('disabled', true);   // Excel Upload Button wird deaktiviert, Benutzer kann keine weitere Datei zusätzlich hochladen
                    $("#modalText").text("Daten werden geladen, bitte warten...");      // Modal Fenster Anzeige Status Meldung
                    $("#uploadModal").show();                       // Modal Fenster

                    var formData = new FormData();              // formData Objekt erzeugt um Datei für den Upload zum Server vorzubereiten
                    formData.append("file", file);

                    fetch("/excel-upload", {                    // Fetch Anfrage um die Datei auf dem Server zu laden, Anfrage am Endpunkt /excel-upload
                        method: "POST",
                        body: formData
                    }).then(function(response) {                // Anfrage Response ok?
                        if (!response.ok) {
                            throw new Error("Fehler beim Importieren der Daten. Die Datei ist leer oder hat keine Spaltenüberschriften.");
                        }
                        return response.text();
                    }).then(function(text) {
                        $("#modalText").text("Daten wurden vollständig hochgeladen");
                        $("#modalButton").show();
                    }).catch(function(error) {
                        $("#modalText").text(error);
                        $("#modalButton").show();
                    }).finally(function() {
                        $("#excelUpload").prop('disabled', false);
                    });
                } else {
                    alert("Bitte laden Sie eine Exceldatei mit der Endung .xlsx Datei hoch.");
                }
            });

            // Hilfe Button Navigation
           $("a.btn").click(function(event) {
               var buttonId = $(this).attr('id');
               if(buttonId === "hilfeButton") {
                   window.location.href = $(this).attr('href');
                   return; // Frühzeitiger Ausstieg aus der Funktion
               }

                // Funktion um Query Parameter , ob schon ein Fragezeichen vorhanden und dann & Zeichen
               event.preventDefault();
               var date = $("#datepicker").val();
               var uploadIndex = $("#uploadIndex").val();
               if (!date && !uploadIndex) {
                   alert("Bitte wählen Sie ein Datum oder die Anzahl der letzten Uploads aus !");
               } else {
                   var url = $(this).attr('href');
                  if (date) {
                      url += (url.indexOf("?") === -1 ? "?" : "&") + "date=" + date;
                  }
                  else if (uploadIndex) {
                      url += (url.indexOf("?") === -1 ? "?" : "&") + "uploadIndex=" + uploadIndex;
                  }
                   window.location.href = url;
               }
           });

        });

        function closeModal() {
            $("#uploadModal").hide();
            $("#modalButton").hide();
        }

