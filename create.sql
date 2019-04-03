/* DROP TABLE SONGS; */ /* used to drop table first if needed */

CREATE TABLE IF NOT EXISTS SONGS  (
    id bigint auto_increment,
    artist varchar(255),
    year varchar(255),
    album varchar(255),
    title varchar(255),
    genre varchar(255)
);


delete from SONGS; /* empties database at end of every program run
					  delete to maintain data between times running program */
