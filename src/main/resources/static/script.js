$(function() {
            $("#datepicker").datepicker({
                dateFormat: "yy-mm-dd",
                changeMonth: true,
                changeYear: true
            });


            $("#excelUpload").change(function() {
                var file = this.files[0];
                if (file && file.name.endsWith(".xlsx")) {
                    $("#excelUpload").prop('disabled', true);
                    $("#modalText").text("Daten werden geladen, bitte warten...");
                    $("#myModal").show();

                    var formData = new FormData();
                    formData.append("file", file);

                    fetch("/excel-upload", {
                        method: "POST",
                        body: formData
                    }).then(function(response) {
                        if (!response.ok) {
                            throw new Error("Fehler beim Hochladen der Excel-Datei.Die Datei ist leer oder hat keine Spaltennamen.");
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
                    alert("Bitte laden Sie eine .xlsx Datei hoch.");
                }
            });

           $("a.btn").click(function(event) {
               var buttonId = $(this).attr('id');
               if(buttonId === "hilfeButton") {
                   window.location.href = $(this).attr('href');
                   return; // Frühzeitiger Ausstieg aus der Funktion
               }

               event.preventDefault();
               var date = $("#datepicker").val();
               var uploadIndex = $("#uploadIndex").val();
               if (!date && !uploadIndex) {
                   alert("Bitte wählen Sie ein Datum oder die Anzahl der letzten Uploads aus !");
               } else {
                   var url = $(this).attr('href');
                   if (date) {
                       url += "?date=" + date;
                   }
                   if (uploadIndex) {
                       url += (date ? "&" : "?") + "uploadIndex=" + uploadIndex;
                   }
                   window.location.href = url;
               }
           });

        });

        function closeModal() {
            $("#myModal").hide();
            $("#modalButton").hide();
        }

