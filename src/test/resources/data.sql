CREATE TABLE IF NOT EXISTS movie (
    id BIGINT primary key generated always as identity,
    title VARCHAR(255) NOT NULL,
    director VARCHAR(255),
    production_year INT,
    ranking INT,
    size_in_bytes BIGINT,
    local_file_path VARCHAR(500)
    );

insert into movie(title,director,production_year,ranking,size_in_bytes,local_file_path)
values ('title1','director1',2001,50,44749826,'D:\movies\VID1.mp4');

insert into movie(title,director,production_year,ranking,size_in_bytes,local_file_path)
values ('title2','director2',2001,150,4474982,'D:\movies\VID2.mp4');

insert into movie(title,director,production_year,ranking,size_in_bytes,local_file_path)
values ('title3','director3',2001,300,44749,'D:\movies\VID3.mp4');

insert into movie(title,director,production_year,ranking,size_in_bytes,local_file_path)
values ('title4','director4',2001,200,447498,'D:\movies\VID4.mp4');




