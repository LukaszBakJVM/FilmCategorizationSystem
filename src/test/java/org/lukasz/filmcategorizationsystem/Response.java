package org.lukasz.filmcategorizationsystem;

public class Response {
    String ranking = """
            [{"title":"title1","director":"director1","productionYear":2001,"ranking":50,"size":44749826},{"title":"title2","director":"director2","productionYear":2001,"ranking":150,"size":4474982},{"title":"title4","director":"director4","productionYear":2001,"ranking":200,"size":447498},{"title":"title3","director":"director3","productionYear":2001,"ranking":300,"size":44749}]
            
            """;

    String sortFields = """
            ["ranking","film_size"]
            """;

    String film_size = """
            [{"title":"title3","director":"director3","productionYear":2001,"ranking":300,"size":44749},{"title":"title4","director":"director4","productionYear":2001,"ranking":200,"size":447498},{"title":"title2","director":"director2","productionYear":2001,"ranking":150,"size":4474982},{"title":"title1","director":"director1","productionYear":2001,"ranking":50,"size":44749826}]
          """;

    String id = """
            [{"title":"title1","director":"director1","productionYear":2001,"ranking":50,"size":44749826},{"title":"title2","director":"director2","productionYear":2001,"ranking":150,"size":4474982},{"title":"title3","director":"director3","productionYear":2001,"ranking":300,"size":44749},{"title":"title4","director":"director4","productionYear":2001,"ranking":200,"size":447498}]
            """;

}
