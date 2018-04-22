package com.github.guymers.fs2.postgresql.messages.decoders

trait AllDecoders
  extends AuthenticationMessageDecoder
  with BackendKeyDataDecoder
  with CommandCompleteDecoder
  with EmptyQueryResponseDecoder
  with ErrorResponseDecoder
  with NegotiateProtocolVersionDecoder
  with NoticeResponseDecoder
  with NotificationResponseDecoder
  with ParameterStatusDecoder
  with ReadyForQueryDecoder
