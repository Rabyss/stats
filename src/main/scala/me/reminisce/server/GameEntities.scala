package me.reminisce.server


import me.reminisce.server.GameEntities.QuestionKind.QuestionKind
import me.reminisce.server.GameEntities.SpecificQuestionType.SpecificQuestionType
import me.reminisce.server.GameEntities.TimeUnit.TimeUnit
import me.reminisce.server.GameEntities.SubjectType.SubjectType
import me.reminisce.server.domain.RestMessage


object GameEntities {

  object SpecificQuestionType extends Enumeration {
    type SpecificQuestionType = Value
    val TLWhenDidYouShareThisPost = Value("TLWhenDidYouShareThisPost")
    val TLWhenDidYouLikeThisPage = Value("TLWhenDidYouLikeThisPage")
    val GeoWhatCoordinatesWereYouAt = Value("GeoWhatCoordinatesWereYouAt")
    val MCWhoMadeThisCommentOnYourPost = Value("MCWhoMadeThisCommentOnYourPost")
    val MCWhichPageDidYouLike = Value("MCWhichPageDidYouLike")
    val MCWhoLikedYourPost = Value("MCWhoLikedYourPost")
    val ORDPageLikes = Value("ORDPageLikes")
    val ORDPostCommentsNumber = Value("ORDPostCommentsNumber")
    val ORDPostLikesNumber = Value("ORDPostLikesNumber")
    val ORDPostTime = Value("ORDPostTime")
    val ORDPageLikeTime = Value("ORDPageLikeTime")
  }

  object QuestionKind extends Enumeration {
    type QuestionKind = Value
    val MultipleChoice = Value("MultipleChoice")
    val Timeline = Value("Timeline")
    val Geolocation = Value("Geolocation")
    val Order = Value("Order")
    val Misc = Value("Misc")
  }

  /**
    * Time units used in Timeline questions
    */
  object TimeUnit extends Enumeration {
    type TimeUnit = Value
    val Day = Value("Day")
    val Week = Value("Week")
    val Month = Value("Month")
    val Year = Value("Year")
  }

  object SubjectType extends Enumeration {
    type SubjectType = Value
    val PageSubject = Value("Page")
    val TextPost = Value("TextPost")
    val ImagePost = Value("ImagePost")
    val VideoPost = Value("VideoPost")
    val LinkPost = Value("LinkPost")
    val CommentSubject = Value("Comment")
  }

  case class Game(gameId: String,
                  player1Id: String,
                  player2Id: String,
                  player1Board: Board,
                  player2Board: Board,
                  status: String, ///??? not sure
                  playerTurn: Int,
                  player1Scores: Int,
                  player2Scores: Int,
                  boardState: List[State],
                  player1AvailableMoves: List[Move],
                  player2AvailableMoves: List[Move],
                  wonBy: Int,
                  creationTime: Int //how big ???
               )

  case class Move(row: Int,
                  col: Int)

  case class State(score: List[Score])

  case class Score(player: Int,
                   score: Int)
  /**
    * Abstract subject, a subject represents a facebook item
    *
    * @param `type` type of the subject
    */
  abstract sealed class Subject(`type`: SubjectType)

  abstract sealed class PostSubject(`type`: SubjectType, text: String, from: Option[FBFrom]) extends Subject(`type`)

  case class PageSubject(name: String, pageId: String,
                         photoUrl: Option[String],
                         `type`: SubjectType = SubjectType.PageSubject) extends Subject(`type`)

  case class TextPostSubject(text: String, `type`: SubjectType = SubjectType.TextPost,
                             from: Option[FBFrom]) extends PostSubject(`type`, text, from)

  case class ImagePostSubject(text: String, imageUrl: Option[String], facebookImageUrl: Option[String],
                              `type`: SubjectType = SubjectType.ImagePost,
                              from: Option[FBFrom]) extends PostSubject(`type`, text, from)

  case class VideoPostSubject(text: String, thumbnailUrl: Option[String], url: Option[String],
                              `type`: SubjectType = SubjectType.VideoPost,
                              from: Option[FBFrom]) extends PostSubject(`type`, text, from)

  case class LinkPostSubject(text: String, thumbnailUrl: Option[String], url: Option[String],
                             `type`: SubjectType = SubjectType.LinkPost,
                             from: Option[FBFrom]) extends PostSubject(`type`, text, from)

  case class CommentSubject(comment: String, post: PostSubject, `type`: SubjectType = SubjectType.CommentSubject) extends Subject(`type`)


  /**
    * Abstract game question
    *
    * @param userId user for which the question is
    * @param kind kind of question (See [[me.reminisce.server.GameEntities.QuestionKind]]
    * @param `type` type of question (See [[me.reminisce.server.GameEntities.SpecificQuestionType]]
    * @param subject subject of the question (See [[me.reminisce.server.GameEntities.Subject]]
    */
  abstract sealed class GameQuestion(userId: String, kind: QuestionKind, `type`: SpecificQuestionType, subject: Option[Subject])

  case class MultipleChoiceQuestion(userId: String,
                                    kind: QuestionKind,
                                    `type`: SpecificQuestionType,
                                    subject: Option[Subject],
                                    choices: List[Possibility],
                                    answer: Int) extends GameQuestion(userId, kind, `type`, subject)

  case class TimelineQuestion(userId: String,
                              kind: QuestionKind,
                              `type`: SpecificQuestionType,
                              subject: Option[Subject],
                              answer: String, // Weird problem with DateTime format serialization
                              min: String,
                              max: String,
                              default: String,
                              unit: TimeUnit,
                              step: Int,
                              threshold: Int) extends GameQuestion(userId, kind, `type`, subject)

  case class OrderQuestion(userId: String,
                           kind: QuestionKind,
                           `type`: SpecificQuestionType,
                           subject: Option[Subject],
                           choices: List[SubjectWithId],
                           answer: List[Int]
                          ) extends GameQuestion(userId, kind, `type`, subject)

  case class SubjectWithId(subject: Subject, uId: Int)


  case class Possibility(name: String, imageUrl: Option[String], `type`: String, fbId: Option[String] = None)

  case class GeolocationQuestion(userId: String,
                                 kind: QuestionKind,
                                 `type`: SpecificQuestionType,
                                 subject: Option[Subject],
                                 answer: Location,
                                 defaultLocation: Location,
                                 range: Double) extends GameQuestion(userId, kind, `type`, subject)

  case class Location(latitude: Double, longitude: Double)

  case class Tile(`type`: QuestionKind,
                  question1: GameQuestion,
                  question2: GameQuestion,
                  question3: GameQuestion) extends RestMessage

  case class Board(userId: String, tiles: List[Tile], isTokenStale: Boolean, strategy: String) extends RestMessage
  case class FBFrom(userId: String, userName: String)


}