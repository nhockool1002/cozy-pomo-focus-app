import { ApiProperty } from '@nestjs/swagger';
import { Type } from 'class-transformer';
import { ArrayMaxSize, IsArray, IsIn, IsObject, IsString, ValidateNested } from 'class-validator';

export const SYNC_EVENT_TYPES = [
  'session_create',
  'session_complete',
  'session_give_up',
  'shop_purchase',
] as const;
export type SyncEventType = (typeof SYNC_EVENT_TYPES)[number];

export class SyncEventDto {
  @ApiProperty({ description: 'ID sinh trên máy, dùng để chống xử lý trùng' })
  @IsString()
  clientEventId!: string;

  @ApiProperty({ enum: SYNC_EVENT_TYPES })
  @IsIn(SYNC_EVENT_TYPES)
  type!: SyncEventType;

  @ApiProperty({ description: 'Payload tương ứng từng loại event, xem docs/technical-spec.md §4.4' })
  @IsObject()
  payload!: Record<string, unknown>;
}

export class SyncBatchDto {
  @ApiProperty({ type: [SyncEventDto] })
  @IsArray()
  @ArrayMaxSize(100)
  @ValidateNested({ each: true })
  @Type(() => SyncEventDto)
  events!: SyncEventDto[];
}
